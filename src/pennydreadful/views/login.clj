(ns pennydreadful.views.login
  (:use net.cgrand.enlive-html)
  (:require [pennydreadful.views.base :as views.base]
            [pennydreadful.util :as util]))

(def template-path "pennydreadful/views/templates/login.html")

(def cljs-launch-ns "pennydreadful.client.login")


(deftemplate login-page template-path [context]
  [:head] (substitute (views.base/base-head cljs-launch-ns))
  [:section.deps] (substitute (views.base/base-deps))
  [:#pd-version] (content (str "v" util/version)))

(defn render [context]
  (apply str (login-page context)))
