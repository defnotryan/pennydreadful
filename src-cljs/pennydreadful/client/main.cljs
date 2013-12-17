(ns pennydreadful.client.main)

(defn ready [f]
  (.ready (js/$ js/document) f))
