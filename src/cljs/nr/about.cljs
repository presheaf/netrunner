(ns nr.about
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [nr.ajax :refer [GET]]
            [nr.appstate :refer [app-state]]
            [reagent.core :as r]))

(def about-state (r/atom {}))

(go (swap! about-state assoc :donators (:json (<! (GET "/data/donors")))))

(defn about []
  (r/with-let [active (r/cursor app-state [:active-page])]
    (when (= "/about" (first @active))
      (let [donators (r/cursor about-state [:donators])]
        (fn []
          [:div.about.panel.content-page.blue-shade
           [:h3 "About"]
           [:p "This is a lightly modified version of " [:a {:href "http://jinteki.net" :target "_blank"} "Jinteki.net"]
            "a website founded by " [:a {:href "http://twitter.com/mtgred" :target "_blank"} "@mtgred"]
            ", an avid Netrunner player from Belgium. For up-to-date information about contributors etc., please see the original site. "
            "All credits, rights et cetera to the source code are entirely due to the original creators and contributors to Jinteki.net. "]
           [:p "The purpose of this page is to facilitate playtesting of an (unofficial) version of Netrunner, where the early cards in the game "
            "have been rebalanced. Changes have been kept minor where possible."
            " For discussion of the format, see #netrunner-reboot-project on Stimslack, and for a list of all card changes, see "
            [:a {:href "https://docs.google.com/spreadsheets/d/19GaP9AwNpvd-gC4sKNo7ZaxRYljb485Uno5VHf9p8As/edit#gid=1391682776" :target "_blank"} "this spreadsheet."]]

           [:h3 "Acknowledgements"]
           [:p "Thanks to Skippan made the image templates used for card generation."]
           [:p "Thanks to NoahTheDuke for being knowledgable and helpful in general, and for answering all kinds of "
            "questions I had about Jinteki internals for this project in particular."]

           [:h3 "Disclaimer"]
           [:p "Netrunner and Android are trademarks of Fantasy Flight Publishing, Inc. and/or Wizards of the Coast LLC."]
           [:p "This is website is not affiliated with Fantasy Flight Games or Wizards of the Coast."]
           [:p "Targeting icon made by "
            [:a {:href "http://www.freepik.com" :title "Freepik" :target "_blank"} "Freepik"]
            " from "
            [:a {:href "http://www.flaticon.com" :title "Flaticon" :target "_blank"} "www.flaticon.com"]
            " is licensed under "
            [:a {:href "http://creativecommons.org/licenses/by/3.0/" :title "Creative Commons BY 3.0" :target "_blank"} "CC BY 3.0"]]])))))
