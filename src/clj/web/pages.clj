(ns web.pages
  (:require [clj-time.core :as t]
            [clj-time.coerce :as c]
            [cheshire.core :as json]
            [hiccup.page :as hiccup]
            [monger.collection :as mc]
            [monger.operators :refer :all]
            [monger.result :refer [acknowledged?]]
            [web.db :refer [db object-id]]
            [web.config :refer [server-config]]
            [web.utils :refer [response]]))

(defn layout [{:keys [version user] :as req} & content]
  (hiccup/html5
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=0.6, minimal-ui"}]
     [:meta {:name "apple-mobile-web-app-capable" :content "yes"}]
     [:title "Reteki"]
     (hiccup/include-css "/css/carousel.css")
     (hiccup/include-css (str "/css/netrunner.css?v=" version))
     (hiccup/include-css "/lib/toastr/toastr.min.css")
     (hiccup/include-css "/lib/jqueryui/themes/base/jquery-ui.min.css")]
    [:body
     content
     (hiccup/include-js "/lib/jquery/jquery.min.js")
     (hiccup/include-js "/lib/jqueryui/jquery-ui.min.js")
     (hiccup/include-js "/lib/bootstrap/dist/js/bootstrap.js")
     (hiccup/include-js "/lib/moment/min/moment.min.js")
     (hiccup/include-js "/lib/marked/marked.min.js")
     (hiccup/include-js "/lib/toastr/toastr.min.js")
     (hiccup/include-js "/lib/howler/dist/howler.min.js")
     (hiccup/include-js "https://browser.sentry-cdn.com/4.1.1/bundle.min.js")
     [:script {:type "text/javascript"}
      (str "var user=" (json/generate-string user) ";")]

     (when-let [sentry-dsn (:sentry-dsn server-config)]
       [:script {:type "text/javascript"}
        (str "Sentry.init({ dsn: '" sentry-dsn "' });"
             (when user
               (str "Sentry.configureScope((scope) => {scope.setUser({\"username\": \""
                    (:username user)
                    "\"});});")))])

     (if (= "dev" @web.config/server-mode)
       (list (hiccup/include-js "/cljs/goog/base.js")
             (hiccup/include-js (str "cljs/app10.js?v=" version))
             [:script
              (for [req ["dev.figwheel"]]
                (str "goog.require(\"" req "\");"))])
       (list (hiccup/include-js (str "js/app10.js?v=" version))
             [:script
              "(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)})(window,document,'script','//www.google-analytics.com/analytics.js','ga');
              ga('create', 'UA-20250150-2', 'www.jinteki.net');"
              (when user
                (str "ga('set', '&uid', '" (:username user) "');"))
              "ga('send', 'pageview');"]))]))


(defn index-page [req]
  (layout
    req

     [:nav.topnav.blue-shade
      [:div#left-menu]
      [:div#right-menu]
      [:div#status]]
     [:div#auth-forms]
     [:div#main.carousel.slide {:data-interval "false"}
      [:div.carousel-inner
       [:div.item.active
        [:div.home-bg]
        [:div.container
         [:h1 "Play Android: Netrunner in your browser"]
         [:div#news]
         [:div#chat]]]
       [:div.item
        [:div.cardbrowser-bg]
        [:div#cardbrowser]]
       [:div.item
        [:div.deckbuilder-bg]
        [:div.container
          [:div#deckbuilder]]]
       [:div.item
        [:div#gamelobby]
        [:div#gameboard]]
       [:div.item
        [:div.help-bg]
        [:div#help]]
       [:div.item
        [:div.account-bg]
        [:div#account]]
       [:div.item
        [:div.stats-bg]
        [:div#stats]]
       [:div.item
        [:div.about-bg]
        [:div#about]]]]
    [:audio#ting
      [:source {:src "/sound/ting.mp3" :type "audio/mp3"}]
     [:source {:src "/sound/ting.ogg" :type "audio/ogg"}]]))

(defn announce-page [req]
  (hiccup/html5
    [:head
     [:title "Announce"]
     (hiccup/include-css "/css/netrunner.css")]
    [:body
     [:div.reset-bg]
     [:form.panel.blue-shade.reset-form {:method "POST"}
      [:h3 "Announcement"]
      [:p
       [:textarea.form-control {:rows 5 :style "height: 80px; width: 250px"
                                :name "message" :autofocus true :required "required"}]]
      [:p
       [:button.btn.btn-primary {:type "submit"} "Submit"]]]]))

(defn version-page [{:keys [version] :as req}]
  (hiccup/html5
    [:head
     [:title "App Version"]
     (hiccup/include-css "/css/netrunner.css")]
    [:body
     [:div.reset-bg]
     [:form.panel.blue-shade.reset-form {:method "POST"}
      [:h3 "App Version"]
      [:p
       [:input {:type "text" :name "version" :value version}]]
      [:p
       [:button.btn.btn-primary {:type "submit"} "Submit"]]]]))

(defn fetch-page [req]
  (hiccup/html5
    [:head
     [:title "Update Card Data"]
     (hiccup/include-css "/css/netrunner.css")]
    [:body
     [:div.reset-bg]
     [:form.panel.blue-shade.reset-form {:method "POST"}
      (when-let [card-info (mc/find-one-as-map db "config" {})]
        [:div.admin
         [:div
          [:h3 "Card Version:"]
          (:cards-version card-info)]
         [:div
          [:h3 "Last Updated:"]
          (:last-updated card-info)]])
      [:br]
      [:button.btn.btn-primary {:type "submit"} "Fetch Cards"]]]))

(defn reset-password-page
  [{{:keys [token]} :params}]
  (if-let [user (mc/find-one-as-map db "users" {:resetPasswordToken   token
                                                :resetPasswordExpires {"$gt" (c/to-date (t/now))}})]
    (hiccup/html5
      [:head
       [:title "Reteki"]
       (hiccup/include-css "/css/netrunner.css")]
      [:body
       [:div.reset-bg]
       [:form.panel.blue-shade.reset-form {:method "POST"}
        [:h3 "Password Reset"]
        [:p
         [:input.form-control {:type "password" :name "password" :value "" :placeholder "New password" :autofocus true :required "required"}]]
        [:p
         [:input.form-control {:type "password" :name "confirm" :value "" :placeholder "Confirm password" :required "required"}]]
        [:p
         [:button.btn.btn-primary {:type "submit"} "Update Password"]]]])
    (response 404 {:message "Sorry, but that reset token is invalid or has expired."})))
