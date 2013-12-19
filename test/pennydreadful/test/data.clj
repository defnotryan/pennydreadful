(ns pennydreadful.test.data
  (:require [expectations :refer :all]
            [datomic.api :as d]
            [clj-time.core :as time]
            [pennydreadful.test.util :refer :all]
            [pennydreadful.data.datomic :as data]
            [pennydreadful.data.user :as data-user]))


(def project-simple
  {:name "my new project"
   :description "some description"})

(def project-other
  {:name "another project"
   :description "some other description"})

(def user-ryan
  {:username "ryan"})

(def user-rhea
  {:username "rhea"})

;; add a user

(expect
 "ryan"
 (with-empty-db
   (let [inserted-user (data/insert-user user-ryan)]
     (:username inserted-user))))

;; add a user - retrieve by username
(expect
 "rhea"
 (with-empty-db
   (data/insert-user user-rhea)
   (:username (data-user/user-for-username "rhea"))))

;; add a project
(expect
 "my new project"
 (with-empty-db
   (let [user (data/insert-user user-ryan)
         project (data/insert-project user project-simple)]
     (:name project))))

;; add a project - retrieve by user
(expect
 #{"my new project" "another project"}
 (with-empty-db
   (let [user (data/insert-user user-ryan)]
     (data/insert-project user project-simple)
     (data/insert-project user project-other)
     (into #{} (map :name (data/projects-for-user user))))))

;; add a project for multiple users - retrieve by user
(expect
 #{"ryan's project 1" "ryan's project 2"}
 (with-empty-db
  (let [ryan (data/insert-user user-ryan)
       rhea (data/insert-user user-rhea)]
   (data/insert-project ryan {:name "ryan's project 1" :description "a"})
   (data/insert-project rhea {:name "rhea's project 1" :description "b"})
   (data/insert-project ryan {:name "ryan's project 2" :description "c"})
   (data/insert-project rhea {:name "rhea's project 2" :description "d"})
   (into #{} (map :name (data/projects-for-user ryan))))))

;; test data - kick the tires
(expect
 #{"dwindling dandies" "condescending cougars"}
 (with-populated-db
   (let [rhea (data-user/user-for-username "rhea")]
     (into #{} (map :name (data/projects-for-user rhea))))))


(expect
 #{"accidental astronauts manuscript" "accidental astronauts research" "accidental astronauts brainstorming"}
 (with-populated-db
   (let [ryan (data-user/user-for-username "ryan")
         accidental-astronauts-project (first (filter #(= "accidental astronauts" (:name %)) (data/projects-for-user ryan)))]
     (data/insert-collection accidental-astronauts-project {:name "accidental astronauts brainstorming"})
     (into #{} (map :name (data/collections-for-project-eid (:id accidental-astronauts-project)))))))
