(ns pennydreadful.test.data
  (:require [expectations :refer :all]
            [datomic.api :as d]
            [clj-time.core :as time]
            [pennydreadful.test.util :refer :all]
            [pennydreadful.data.datomic :as data]
            [pennydreadful.data.user :as data-user]
            [pennydreadful.data.project :as data-project]))


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

;; test data - kick the tires
(expect
 #{"dwindling dandies" "condescending cougars"}
 (with-populated-db
   (let [rhea (data-user/user-for-username "rhea")]
     (into #{} (map :name (data-project/projects-for-user-eid (:id rhea)))))))


(expect
 #{"accidental astronauts manuscript" "accidental astronauts research" "accidental astronauts brainstorming"}
 (with-populated-db
   (let [ryan (data-user/user-for-username "ryan")
         accidental-astronauts-project (first (filter #(= "accidental astronauts" (:name %)) (data-project/projects-for-user-eid (:id ryan))))]
     (data/insert-collection accidental-astronauts-project {:name "accidental astronauts brainstorming"})
     (into #{} (map :name (data/collections-for-project-eid (:id accidental-astronauts-project)))))))
