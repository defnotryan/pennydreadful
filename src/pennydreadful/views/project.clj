(ns pennydreadful.views.project
  (:use net.cgrand.enlive-html)
  (:require [pennydreadful.views.base :as views.base]
            [pennydreadful.util :as util]))

(def template-path "pennydreadful/views/templates/project.html")

(def cljs-launch-ns "pennydreadful.client.project.ui")

(deftemplate project-page template-path [context]
  [:head] (substitute (views.base/base-head cljs-launch-ns))
  [:nav] (substitute (views.base/base-nav (:username context)))
  [:section.deps] (substitute (views.base/base-deps)))

(defn render [context]
  (apply str (project-page context)))
