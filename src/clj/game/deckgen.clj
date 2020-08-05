(ns game.deckgen
  (:require [clojure.java.io :as io]
            [jinteki.cards :refer [all-cards]]

            [game.utils :refer [server-card]]))

(declare load-deck-stubs!)

(def jumpstart-tags-filename "data/jumpstart-cardtags.edn")
(def jumpstart-templates-filename "data/jumpstart-templates.edn")
(def all-card-tags (load-file jumpstart-tags-filename))
(def templates-by-side (load-file jumpstart-templates-filename))

(def my-template (first (:corp templates-by-side)))

(def cards-by-tag
  ;; map tag -> [c1, c2, ...] of all such tagged cards
  (reduce-kv (fn [coll cardname tags]
               ;; want (assoc coll t1 cardname t2 cardname ...)
               (reduce (fn [m tag] (update coll tag #(conj % cardname))) coll tags))
             {} all-card-tags))

(def stubs-corp-filename "data/deck-stubs-corp.edn")
(def stubs-runner-filename "data/deck-stubs-runner.edn")

(def deck-stubs (atom {}))


(defn gen-template
  "Given a template, randomly choose an ID, then fill it with cards. Remove stuff from the template as you go."
  ([template]
   (gen-template (dissoc template :identity) {:identity (get @all-cards (rand-nth (:identity template)))
                                              :cards []}))
  ([template partial-deck]
   (let [deck-id               (:identity partial-deck)
         target-decksize       (+ (:minimumdecksize deck-id) (if (= "Corp" (:side deck-id))
                                                               4 0))
         target-ap              (+ 2 (* 2 (quot target-decksize 5)))
         inf-limit             (:influencelimit deck-id)
         current-ap            (reduce + (filter some? (map :agendapoints (:cards partial-deck))))
         spent-inf             (reduce + (filter some? (map #(if (= (:faction deck-id) (:faction %))
                                                               0 (:factioncost %))
                                                            (:cards partial-deck))))
         remaining-inf         (- inf-limit spent-inf)
         remaining-ap          (- target-ap current-ap)]
     (if (<= target-decksize (count (:cards partial-deck)))
       ;; if we're done, then we're done
       partial-deck
       
       ;; choose a new random card matching one of the tags in our deck which doesn't take us over inf and keep going
       (let [admissible-card 
             (if (and (= "Corp" (:side deck-id))
                      (< 0 remaining-ap))
               ;; ensure we fill up agenda points first, so only look at agendas we can play which aren't 0 pts or takes us over 20
               (fn [card] (and (= "Agenda" (:type card))
                              (#{"Neutral" (:faction deck-id)} (:faction card))
                              (get all-card-tags (:title card)) ; ensures we don't grab from outside cardpool
                              (< 0 (:agendapoints card))
                              (<= (:agendapoints card) remaining-ap)))
               
               ;; we're free to do whatever we want now, so choose a random tag in the template and go nuts
               (let [desired-tags (filter #(< 0 (get (:tags template) %))
                                          (keys (:tags template)))
                     current-step (first (filter #(some (set desired-tags) %) (:steps template)))
                     target-tag (do ;; (prn (str "current step: " current-step))
                                    
                                    (when current-step
                                      (rand-nth (filter (set desired-tags)
                                                        current-step))))]
                 (if target-tag
                   (do ;; (prn (str "choosing a " target-tag))
                     (fn [card]
                         (some #(= target-tag %) (get all-card-tags (:title card)))))
                   (do ;; (prn "choosing anything?!")
                       (fn [card] (get all-card-tags (:title card)))))))
             chosen-card (rand-nth (filter (fn [card]
                                             (and (= (:side deck-id) (:side card))
                                                  (admissible-card card)
                                                  (do (or (= (:faction card) (:faction deck-id))
                                                          (and (:factioncost card)
                                                               (<= (:factioncost card) remaining-inf))))))
                                           (vals @all-cards)))
             card-tags       (get all-card-tags (:title chosen-card))]
         ;; (prn (str "chose " (:title chosen-card) " with tags " card-tags))
         ;; having chosen a random card, add it to the deck, decrement all the tags it satisfies in the template by 1 and move on
         (let [new-deck     (update partial-deck :cards #(conj % chosen-card))
               new-tags     (reduce (fn [tmpl tag]
                                      (if (get tmpl tag)
                                        (update tmpl tag dec)
                                        tmpl))
                                    (:tags template)
                                    (get all-card-tags (:title chosen-card)))]
           (gen-template (assoc template :tags new-tags) new-deck)))))))

(def my-deck (gen-template my-template))
(prn (:identity my-deck))
(prn (map :title (:cards my-deck)))
(prn (map #(vector (:faction %)
                   (:factioncost %)) (:cards my-deck)))
(prn (count (:cards my-deck)))

(defn parse-stub
  [stub]
  (cond (string? stub)
        (list stub)

        (vector? stub)
        (apply concat (map parse-stub stub))

        (map? stub)              ;in this case, stub is {num: subtmpl}
        (parse-stub (apply vector (repeat (first (first stub))
                                              (second (first stub)))))

        (set? stub)
        (parse-stub (rand-nth (seq stub)))

        :else '()))


(defn generate-deck
  [side]
  (if (= 0 (count @deck-stubs))
    (load-deck-stubs!))                 ;TODO: move this to start time so wrong names can be detected
  (let [cardlist (sort (apply concat (map (fn [stub] (parse-stub (:cards stub)))
                                          (take 3 (shuffle (side @deck-stubs))))))]
    (apply vector (map (fn [[title qty]] {:qty qty :card {:title title}})
                       (frequencies cardlist)))))


(defn verify-stub
  [stub]
  (cond (string? stub)
        (let [is-valid (get @all-cards stub)]
          (when-not is-valid
            (prn (str "Invalid card title " stub)))
          (list is-valid))

        (map? stub)
        (verify-stub (second (first stub)))

        :else
        (apply concat (map verify-stub stub))))

(defn load-deck-stubs!
  []
  ;; TODO: replace load-file here by EDN loader and consider using hawk to watch the stuff
  ;; TODO: clean up verification method so it prints the name of the offending file
  (let [stubs-corp   (when (.exists (io/file stubs-corp-filename))
                       (load-file stubs-corp-filename))
        stubs-runner (when (.exists (io/file stubs-runner-filename))
                       (load-file stubs-runner-filename))]
    (if (every? #(every? boolean
                         (flatten (map (fn [d] (verify-stub (:cards d))) %)))
                [stubs-corp stubs-runner])
      (do (prn "Successfully loaded stubs!")
          (reset! deck-stubs {:corp stubs-corp
                              :runner stubs-runner}))
      (prn "Error loading stubs"))))



