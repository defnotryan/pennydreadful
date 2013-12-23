(ns pennydreadful.views.base
  (:use net.cgrand.enlive-html)
  (:require [pennydreadful.util :as util]))

(def template-path "pennydreadful/views/templates/base.html")

(defsnippet base-head template-path [:head] [cljs-launch-ns]
  [[:link (attr-ends :href "foundation.css")]] (set-attr :href "/css/foundation.css")
  [[:link (attr-ends :href "site.css")]] (set-attr :href "/css/site.css")
  [[:script (attr-ends :src "modernizr.js")]] (set-attr :src "/js/modernizr.js")
  [[:script (attr-ends :src "jquery.js")]] (set-attr :src "/js/jquery.js")
  [[:script (attr-ends :src "underscore.min.js")]] (set-attr :src "/js/underscore.min.js")
  [[:script (attr-ends :src "spin.min.js")]] (set-attr :src "/js/spin.min.js")
  [[:script (attr-ends :src "site.js")]] (set-attr :src "/js/site.js")
  [:script#launch] (content (str cljs-launch-ns ".init()")))

(defsnippet base-nav template-path [:nav] [username]
  [:#pd-version] (content (str "v" util/version))
  [:#username-label] (content username)
  [:.projects-link] (set-attr :href "/project")
  [:.logout-link] (set-attr :href "/logout?msg=success"))

(defsnippet base-deps template-path [:section.deps] []
  [[:script (attr-ends :src "foundation.min.js")]] (set-attr :src "/js/foundation.min.js"))

