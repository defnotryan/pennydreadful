(ns pennydreadful.test.data.project
  (:require [expectations :refer :all]
            [pennydreadful.test.util :refer :all]
            [pennydreadful.data.user :as data-user]
            [pennydreadful.data.project :refer :all]))

;; Retrieve projects
(expect
 #{"accidental astronauts" "bromantic birdfeeders"}
 (with-populated-db
   (let [ryan (data-user/user-for-username "ryan")
         projects (projects-for-user-eid (:id ryan))]
     (into #{} (map :name projects)))))

;; Insert a project
(expect
 #{"accidental astronauts" "bromantic birdfeeders" "catatonic catamounts"}
 (with-populated-db
   (let [ryan (data-user/user-for-username "ryan")
         ryan-eid (:id ryan)]
     (insert-project ryan-eid {:name "catatonic catamounts" :description "desc"})
     (let [projects (projects-for-user-eid ryan-eid)]
       (into #{} (map :name projects))))))

(expect
 {:name "catatonic catamounts" :description "a story about catatonic catamounts"}
 (in
  (with-populated-db
    (let [ryan (data-user/user-for-username "ryan")]
      (insert-project
       (:id ryan)
       {:name "catatonic catamounts"
        :description "a story about catatonic catamounts"})))))
