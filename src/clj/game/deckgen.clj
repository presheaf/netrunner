(ns game.deckgen
  (:require [clojure.java.io :as io]
            [clojure.set :refer [subset?]]
            [jinteki.cards :refer [all-cards]]
            [clojure.string :refer [lower-case]]
            [game.utils :refer [server-card]]))

;; (declare load-deck-stubs!)

(def jumpstart-tags-filename "data/jumpstart-cardtags.edn")
(def jumpstart-templates-filename "data/jumpstart-templates.edn")

(defn  matches-tag [tag cardtags]
  (if (set? tag)
    (subset? tag (into (hash-set) cardtags))
    (some #(= tag %) cardtags)))

(def all-card-tags
  (let [raw-all-card-tags (load-file jumpstart-tags-filename)]
    (apply hash-map (interleave (map lower-case (keys raw-all-card-tags))
                                (vals raw-all-card-tags)))))

(defn tags-of [card]
  (get all-card-tags (lower-case (:title card))))

(def templates-by-side (load-file jumpstart-templates-filename))

(def cards-by-tag
  ;; map tag -> [c1, c2, ...] of all such tagged cards
  (reduce-kv (fn [coll cardname tags]
               ;; want (assoc coll t1 cardname t2 cardname ...)
               (reduce (fn [m tag] (update coll tag #(conj % (lower-case cardname)))) coll tags))
             {} all-card-tags))


(defn generate-from-template
  "Given a template, randomly choose an ID, then fill it with cards. Remove stuff from the template as you go."
  ([template]
   (generate-from-template (dissoc template :identity) (dissoc template :identity)
                           {:identity (get @all-cards (rand-nth (:identity template)))
                            :cards []}
                           20))
  ([orig-template templ p-deck n-retries]
   (loop [template templ
          partial-deck p-deck
          num-retries n-retries]
     (let [deck-id               (:identity partial-deck)
           target-decksize       (+ ;; (:minimumdecksize deck-id)
                                  45
                                  (if (= "Corp" (:side deck-id))
                                    4 0))
           target-ap              (+ 2 (* 2 (quot target-decksize 5)))
           ;; inf-limit             (:influencelimit deck-id)
           current-ap            (reduce + (filter some? (map :agendapoints (:cards partial-deck))))
           ;; spent-inf             (reduce + (filter some? (map #(if (= (:faction deck-id) (:faction %))
           ;;                                                       0 (:factioncost %))
           ;;                                                    (:cards partial-deck))))
           ;; remaining-inf         (- inf-limit spent-inf)
           remaining-ap          (- target-ap current-ap)
           desired-tags          (filter #(< 0 (get (:tags template) %))
                                         (keys (:tags template)))
           current-step (first (filter #(some (set desired-tags) %) (:steps template)))
           target-tag (do ;; (prn (str "current step: " current-step))
                        (if current-step
                          (rand-nth (filter (set desired-tags)
                                            current-step))
                          (rand-nth (:filler template))))]
       (if (<= target-decksize (count (:cards partial-deck)))
         ;; if we're done, check if we got all the tags we needed, and if not maybe start over
         (let [should-retry (and (not (empty? desired-tags))
                                 (< 0 num-retries))
               missing-tags-map (into (hash-map) (filter (fn [kv] (pos? (second kv))) (:tags template)))]
           (do (prn (str (count desired-tags) " not filled (" missing-tags-map   ") - " should-retry))
               (if should-retry
                 (recur orig-template (assoc partial-deck :cards []) (dec num-retries))
                 partial-deck)))
         
         ;; choose a new random card matching one of the tags in our deck which doesn't take us over inf and keep going
         (let [inadmissible-card
               (fn [card] (and
                          (some (set (:never template))
                                (tags-of card))
                          (not (some (set (:unless template))
                                     (tags-of card)))))
               
               admissible-card
               (if (and (= "Corp" (:side deck-id))
                        (< 0 remaining-ap))
                 ;; ensure we fill up agenda points first, so only look at agendas we can play which aren't 0 pts or takes us over 20
                 (do ;; (prn "picking agenda")
                   (fn [card] (and (= "Agenda" (:type card))
                                  (or (= (:faction deck-id) (:faction card))
                                      (and (= "Neutral" (:faction card))
                                           ;; (= 0 (rand-int 2))
                                           )
                                      (= 0 (rand-int 8)))
                                  (or (not= (lower-case (:title card))
                                            "government takeover")
                                      (= 0 rand-int 10))

                                  (tags-of card) ; ensures we don't grab from outside cardpool
                                  (< 0 (:agendapoints card))
                                  (or (and (= 2 (:agendapoints card)) ; make 3/2, 4/2, 5/2 twice as likely
                                           (#{3 4 5} (:advancementcost card)))
                                      (= 0 (rand-int 4)))
                                  (<= (:agendapoints card) remaining-ap))))
                 
                 ;; we're free to do whatever we want now, so choose a random tag in the template and go nuts
                 (if target-tag
                   
                   (do ;; (prn (str "choosing a " target-tag))
                     (fn [card]
                       (let [ret (and (matches-tag target-tag (tags-of card))
                                      (not= (:type card) "Agenda"))]
                         ;; (prn (str "for " (:title card) ": " ret))
                         ret)))
                   (do ;; (prn "choosing anything?!")
                     (fn [card] (and (tags-of card)
                                    (not= (:type card) "Agenda"))))))
               admissible-cards (filter (fn [card]
                                          (and (= (:side deck-id) (:side card))
                                               (not= (:type card) "Identity")
                                               (admissible-card card)
                                               (not (inadmissible-card card))

                                               ;; (do (or (= (:faction card) (:faction deck-id))
                                               ;;         ;; (and true ;; (:factioncost card);; (<= (:factioncost card) remaining-inf)
                                               ;;         ;;      )
                                               ;;         ))
                                               ))
                                        (vals @all-cards))
               chosen-card     (do ;; (prn (str (count admissible-cards) " admissible cards"))

                                   
                                 (if (not (empty? admissible-cards))
                                   (rand-nth admissible-cards)))
               card-tags       (when chosen-card (tags-of chosen-card))]
           ;; (prn (str "chose " (:title chosen-card) " with tags " card-tags))
           ;; having chosen a random card, add it to the deck, decrement all the tags it satisfies in the template by 1 and move on

           ;; if a card is not in-faction, put it back with probability increasing in the inf on the card,
           ;; meaning in-faction cards are more likely to appear than out-of-faction cards, and low-inf cards more likely
           ;; to be splashed than high-inf cards

           ;; (prn chosen-card)
           (if (and chosen-card
                    (or (= (:faction chosen-card) (:faction deck-id))
                        (= (:faction chosen-card) "Neutral")
                        (= 0 (rand-int (+ (get chosen-card :factioncost 5)
                                          (if (= (:side deck-id) "Corp")
                                            4 3))))))
             
             (let [new-deck     (update partial-deck :cards #(conj % chosen-card))
                   card-tags    (tags-of chosen-card)
                   new-tags     (into (hash-map) (map (fn [[tag num]]
                                                        [tag (if (and (pos? num)
                                                                      (matches-tag tag card-tags))
                                                               (dec num)
                                                               num)])
                                                      (:tags template)))
                   ;; (reduce (fn [tmpl tag]
                   ;;                        (if (get tmpl tag)
                   ;;                          (update tmpl tag dec)
                   ;;                          tmpl))
                   ;;                      (:tags template)
                   ;;                      (get all-card-tags (lower-case (:title chosen-card))))
                   ]
               ;; (prn (str "Adding " (:title chosen-card) " with tags " (vector card-tags)))
               (recur (assoc template :tags new-tags) new-deck num-retries))
             (recur template partial-deck num-retries))))))))


(defn generate-deck
  [side]
  (let [template (rand-nth (apply concat (map (fn [tmpl] (repeat (:odds tmpl) tmpl))
                                              (side templates-by-side))))]
    (prn (str "Generating deck from template" template))
    (let [deck        (generate-from-template template)
          deck-id     (:title (:identity deck))
          card-titles (map :title (:cards deck))]
      {:identity deck-id
       :cards (apply vector (map (fn [[title qty]] {:qty qty :card {:title title}})
                                 (frequencies card-titles)))})))

;;; utility
(defn deck-titles
  [d]
  (map :title (:cards d)))
