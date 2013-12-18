(ns pennydreadful.test.data
  (:require [expectations :refer :all]
            [datomic.api :as d]
            [clj-time.core :as time]
            [pennydreadful.data.datomic :refer :all]))

(defn create-empty-in-memory-db []
  (let [uri "datomic:mem://pennydreadful-db"]
    (d/delete-database uri)
    (d/create-database uri)
    (let [conn (d/connect uri)
          schema (load-file "resources/datomic/schema.edn")]
      (d/transact conn schema)
      (atom conn))))

(defn create-populated-in-memory-db []
  (let [uri "datomic:mem://pennydreadful-db"]
    (d/delete-database uri)
    (d/create-database uri)
    (let [conn (d/connect uri)
          schema (load-file "resources/datomic/schema.edn")
          test-data (load-file "resources/datomic/test-data.edn")]
      (d/transact conn schema)
      (d/transact conn test-data)
      (atom conn))))

(def project-simple
  {:name "my new project"
   :description "some description"})

(def project-other
  {:name "another project"
   :description "some other description"})

(def user-ryan
  {:name "ryan"})

(def user-rhea
  {:name "rhea"})

;; add a user
(expect "ryan"
        (with-redefs [conn (create-empty-in-memory-db)]
          (let [inserted-user (insert-user user-ryan)]
            (:name inserted-user))))

;; add a user - retrieve by username
(expect "rhea"
        (with-redefs [conn (create-empty-in-memory-db)]
          (insert-user user-rhea)
          (:name (user-for-username "rhea"))))

;; add a project
(expect "my new project"
        (with-redefs [conn (create-empty-in-memory-db)]
          (let [user (insert-user user-ryan)
                project (insert-project user project-simple)]
            (:name project))))

;; add a project - retrieve by user
(expect #{"my new project" "another project"}
        (with-redefs [conn (create-empty-in-memory-db)]
          (let [user (insert-user user-ryan)]
            (insert-project user project-simple)
            (insert-project user project-other)
            (into #{} (map :name (projects-for-user user))))))

;; add a project for multiple users - retrieve by user
(expect #{"ryan's project 1" "ryan's project 2"}
        (with-redefs [conn (create-empty-in-memory-db)]
          (let [ryan (insert-user user-ryan)
                rhea (insert-user user-rhea)]
            (insert-project ryan {:name "ryan's project 1" :description "a"})
            (insert-project rhea {:name "rhea's project 1" :description "b"})
            (insert-project ryan {:name "ryan's project 2" :description "c"})
            (insert-project rhea {:name "rhea's project 2" :description "d"})
            (into #{} (map :name (projects-for-user ryan))))))

;; test data - kick the tires
(expect #{"dwindling dandies" "condescending cougars"}
        (with-redefs [conn (create-populated-in-memory-db)]
          (let [rhea (user-for-username "rhea")]
            (into #{} (map :name (projects-for-user rhea))))))

(expect #{"accidental astronauts manuscript" "accidental astronauts research" "accidental astronauts brainstorming"}
        (with-redefs [conn (create-populated-in-memory-db)]
          (let [ryan (user-for-username "ryan")
                accidental-astronauts-project (first (filter #(= "accidental astronauts" (:name %)) (projects-for-user ryan)))]
            (insert-collection accidental-astronauts-project {:name "accidental astronauts brainstorming"})
            (into #{} (map :name (collections-for-project-eid (:id accidental-astronauts-project)))))))