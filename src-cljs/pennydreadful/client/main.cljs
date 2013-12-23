(ns pennydreadful.client.main
  (:require [pennydreadful.client.util :refer [requests-outstanding? show-spinner hide-spinner]])
  (:require-macros [tailrecursion.javelin :refer [cell=]]))

(defn ready [f]
  (.ready
   (js/$ js/document)
   (fn []
     (cell=
      (if requests-outstanding?
        (show-spinner)
        (hide-spinner)))
     (f))))
