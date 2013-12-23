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
     (insert-project! ryan-eid {:name "catatonic catamounts" :description "desc"})
     (let [projects (projects-for-user-eid ryan-eid)]
       (into #{} (map :name projects))))))

(expect
 {:name "catatonic catamounts" :description "a story about catatonic catamounts"}
 (in
  (with-populated-db
    (let [ryan (data-user/user-for-username "ryan")]
      (insert-project!
       (:id ryan)
       {:name "catatonic catamounts"
        :description "a story about catatonic catamounts"})))))

;; Ownership
(expect
 identity ;; expect truthy
 (with-populated-db
   (let [ryan (data-user/user-for-username "ryan")
         project (insert-project! (:id ryan) {:name "surreal serpents" :description "sssurreal ssserpentsss"})]
     (project-eid-owned-by-user-eid? (:id project) (:id ryan)))))

;; Delete a project
(expect
 #{"accidental astronauts" "bromantic birdfeeders" "Ke$ha: An autobiography"}
 (with-populated-db
   (let [ryan (data-user/user-for-username "ryan")
         ryan-eid (:id ryan)
         war-and-peace (insert-project! ryan-eid {:name "War and Peace" :description "A very meaningful novel"})
         kesha (insert-project! ryan-eid {:name "Ke$ha: An autobiography" :description "I woke up in the morning feeling like P Diddy..."})]
     (delete-project! (:id war-and-peace))
     (let [projects (projects-for-user-eid ryan-eid)]
       (into #{} (map :name projects))))))

;; Update a project
(expect
 {:name "dauntless demographics" :description "the new description"}
 (in
  (with-populated-db
    (let [{ryan-eid :id} (data-user/user-for-username "ryan")
          {project-eid :id :as project} (insert-project! ryan-eid {:name "dauntless demographics" :description "the old description"})]
      (update-project! (assoc project :description "the new description"))
      (project-by-eid project-eid)))))

(expect
 {:name "new dauntless demographics" :description "the old description"}
 (in
  (with-populated-db
    (let [{ryan-eid :id} (data-user/user-for-username "ryan")
          {project-eid :id :as project} (insert-project! ryan-eid {:name "dauntless demographics" :description "the old description"})]
      (update-project! (assoc project :name "new dauntless demographics"))
      (project-by-eid project-eid)))))

(expect
 {:name "new dauntless demographics" :description "the new description"}
 (in
  (with-populated-db
    (let [{ryan-eid :id} (data-user/user-for-username "ryan")
          {project-eid :id :as project} (insert-project! ryan-eid {:name "dauntless demographics" :description "the old description"})]
      (update-project! (assoc project :name "new dauntless demographics" :description "the new description"))
      (project-by-eid project-eid)))))
