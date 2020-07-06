(ns game.deckgen
  (:require [clojure.java.io :as io]
            [jinteki.cards :refer [all-cards]]

            [game.utils :refer [server-card]]))

(declare load-deck-stubs!)

(def stubs-corp-filename "data/deck-stubs-corp.edn")
(def stubs-runner-filename "data/deck-stubs-runner.edn")

(def deck-stubs (atom {}))



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



