(ns pennydreadful.views.projects
  (:use net.cgrand.enlive-html)
  (:require [pennydreadful.views.base :as views.base]
            [pennydreadful.util :as util]))

(def template-path "pennydreadful/views/templates/projects.html")

(def cljs-launch-ns "pennydreadful.client.projects")

(defsnippet project-panel template-path [:#project-list :> first-child]
  [{project-title :name project-description :description}]
  [:.project-title] (content project-title)
  [:.project-description] (content project-description))

(deftemplate projects-page template-path [context]
  [:head] (substitute (views.base/base-head cljs-launch-ns))
  [:nav] (substitute (views.base/base-nav (:username context)))
  [:section.deps] (substitute (views.base/base-deps))
  [:#project-list] (content (map project-panel (:projects context))))

(defn render [context]
  (apply str (projects-page context)))
