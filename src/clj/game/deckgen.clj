(ns game.deckgen
  (:require [clojure.java.io :as io]
            [jinteki.cards :refer [all-cards]]

            [game.utils :refer [server-card]]))

(declare load-deck-stubs!)

(def stubs-corp-filename "data/deck-stubs-corp.edn")
(def stubs-runner-filename "data/deck-stubs-runner.edn")

(def deck-stubs (atom {}))


(defn gen-template
  "Given a template, randomly choose an ID, then fill it with cards. Remove stuff from the template as you go."
  ([template]
   (gen-template (dissoc :identity template) {:identity (get @all-cards (rand-nth (:identity template)))
                                              :cards []}))
  ([template partial-deck]
   (let [deck-id               (:identity partial-deck)
         target-decksize       (+ (:minimum-deck-size deck-id) (if (= :corp (:side deck-id))
                                                                  4 0))
         target-ap              (+ 2 (* 2 (quot target-decksize 5)))
         inf-limit             (:influence-limit deck-id)
         current-ap            (reduce + (filter some? (map :agenda-points (:cards partial-deck))))
         spent-inf             (reduce + (filter some? (map :influence-cost (:cards partial-deck))))
         remaining-inf         (- inf-limit spent-inf)
         remaining-ap          (- target-ap current-ap)]
     (if (<= 0 (- target-decksize (count (:cards partial-deck))))
       ;; if we're done, then we're done
       partial-deck
       
       ;; choose a new random card matching one of the tags in our deck which doesn't take us over inf and keep going
       (let [admissible-card (if (and (= :corp (:side deck-id))
                                      remaining-ap)
                               ;; ensure we fill up agenda points first, so only look at agendas we can play which aren't 0 pts or takes us over 20
                               (fn [card] (and (= :agenda (:type card))
                                              (#{:neutral-corp (:faction deck-id)} (:faction card))
                                              (:agenda-points card)
                                              (<= (:agenda-points card) remaining-ap)))
                               
                               ;; we're free to do whatever we want now, so choose a random tag in the template and go nuts
                               (let [desired-tags (filter #(< 0 (% (:tags template)))
                                                          (keys (:tags template)))
                                     target-tag (when desired-tags (rand-nth desired-tags))] ;TODO: ensure this isn't weird - does rand-nth work on sets?
                                 ;; TODO: write logic making a predicate which is true iff the card has that tag
                                 nil))
             chosen-card     (rand-nth (filter (fn [card] (and (admissible-card card)
                                                              (or (= (:faction card) (:faction deck-id))
                                                                  (and (:influence-cost card)
                                                                       (<= (:influence-cost card) remaining-inf)))))
                                               @all-cards))
             ;; TODO: add logic making a set of the tags of a card here
             card-tags       nil]

         ;; having chosen a random card, add it to the deck, decrement all the tags it satisfies in the template by 1 and move on
         (let [new-deck     (conj (:cards partial-deck))
               new-template (update template :tags (fn [tagmap]
                                                     (reduce-kv (fn [coll tag num] (if (card-tags tag) (dec num) num))
                                                                {} tagmap)))])
         (gen-template new-template new-deck))))))


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



