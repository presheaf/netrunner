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
            " For discussion of the format, visit "
            [:a {:href " https://discord.gg/YZPgyJwVKFsee" :target "_blank"} "this Discord,"]
            "and for an overview of all changes, see"
            [:a {:href "https://sites.google.com/view/netrunner-reboot-project/" :target "_blank"} "here."]]

           [:h3 "Acknowledgements"]
           [:p "Thanks to Skippan, who significantly improved the image templates used for card generation."]
           [:p "Thanks to NoahTheDuke for being knowledgable and helpful in general, and for answering all kinds of "
            "questions I had about Jinteki internals for this project in particular."]

           [:h3 "Donations"]
           [:p "Although nobody makes any profit from running this page, donators help keep the server "
            "running. If you are interested in helping out, you will get access to full-art versions of the cards as a token of gratitude - see "
            [:a {:href "https://ko-fi.com/reboot_dev" :target "_blank"} "here"] " for details."]

           [:p "Many thanks to all donors!"]
           [:ul.list.compact
            (for [d @donors]
              ^{:key d}
              [:li d])]

           [:h3 "Disclaimer"]
           [:p "Netrunner and Android are trademarks of Fantasy Flight Publishing, Inc. and/or Wizards of the Coast LLC."]
           [:p "This is website is not affiliated with Fantasy Flight Games, Wizards of the Coast or NISEI, and does not profit off any of their intellectual properties."]
           [:p "Targeting icon made by "
            [:a {:href "http://www.freepik.com" :title "Freepik" :target "_blank"} "Freepik"]
            " from "
            [:a {:href "http://www.flaticon.com" :title "Flaticon" :target "_blank"} "www.flaticon.com"]
            " is licensed under "
            [:a {:href "http://creativecommons.org/licenses/by/3.0/" :title "Creative Commons BY 3.0" :target "_blank"} "CC BY 3.0"]]])))))
