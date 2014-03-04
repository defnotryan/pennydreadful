(ns pennydreadful.views.utils
  (:require [net.cgrand.enlive-html :as en]))

(defn attr-cond [bool & kvs]
  (if bool
    (apply en/set-attr kvs)
    (apply en/remove-attr (take-nth 2 kvs))))
