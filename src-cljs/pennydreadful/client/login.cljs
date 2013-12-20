(ns pennydreadful.client.login
  (:require [enfocus.core :as ef]
            [enfocus.events :as ee]
            [pennydreadful.client.main :as main]
            [pennydreadful.client.util :as util])
  (:require-macros [enfocus.macros :refer [defaction]]))

(def failure-alert
  [:div.row
   [:div.large-12.columns
    [:div.alert-box.warning.radius {:data-alert true}
     "There was a problem logging you in. Please try again."]]])

(defn submit-login []
  (let [login-form (.getElementById js/document "login-form")]
    (.submit login-form)))

(defaction login-failure []
  "#username" (ef/set-attr :value (:username util/query-params))
  "#password" (ef/focus)
  "#login-form" (ef/before (ef/html failure-alert)))

(defaction setup-events []
  "#login-button" (ee/listen :click submit-login)
  "#login-form" (ee/listen :keypress #(when (= 13 (.-charCode %))
                                         (submit-login))))

(defaction setup []
  "#username" (ef/focus))

(defn ready []
  (setup)
  (setup-events)
  (when (= "Y" (:login_failed util/query-params))
    (login-failure)))

(defn init []
  (main/ready ready))
