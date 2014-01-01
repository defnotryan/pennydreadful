(ns pennydreadful.client.util-macros
  (:require [cljs.core.async.macros :refer [go]]))

(defmacro go-forever [& body]
  `(go (while true ~@body)))
