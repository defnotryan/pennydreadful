(ns pennydreadful.test.util
  (:require [datomic.api :as d]
            [pennydreadful.data.datomic :as pd-datomic]))

(def test-uri "datomic:mem://pennydreadful-db")

(defn create-db [uri & schema-paths]
  (d/delete-database uri)
  (d/create-database uri)
  (let [conn (d/connect uri)]
    (doseq [schema-path schema-paths]
      (d/transact conn (load-file schema-path)))
    (atom conn)))

(def create-empty-in-memory-db
  (partial create-db test-uri "resources/datomic/schema.edn"))

(defmacro with-empty-db [& body]
  `(with-redefs [pd-datomic/conn (create-empty-in-memory-db)]
     ~@body))

(def create-populated-in-memory-db
  (partial create-empty-in-memory-db "resources/datomic/test-data.edn"))

(defmacro with-populated-db [& body]
  `(with-redefs [pd-datomic/conn (create-populated-in-memory-db)]
     ~@body))