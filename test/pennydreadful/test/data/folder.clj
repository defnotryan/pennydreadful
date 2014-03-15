(ns pennydreadful.test.data.folder
  (:require [expectations :refer :all]
            [clj-time.coerce :as tc]
            [pennydreadful.test.util :refer :all]
            [pennydreadful.util :refer [chain-pprint]]
            [pennydreadful.data.user :as data-user]
            [pennydreadful.data.project :as data-project]
            [pennydreadful.data.collection :as data-coll]
            [pennydreadful.data.folder :refer :all]))

;; Insert folder into collection
(expect
 {:name "chapter 1" :description "they become astronauts accidentally"}
 (in
  (with-populated-db
    (let [{ryan-eid :id} (data-user/user-for-username "ryan")
          {coll-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                             (find-where :name "accidental astronauts")
                             :id
                             (data-project/project-by-eid {:depth :collection})
                             :collections
                             (find-where :name "accidental astronauts manuscript"))]
      (insert-folder-into-collection! coll-eid
                                      {:name "chapter 1"
                                       :description "they become astronauts accidentally"})
      (-> (data-coll/collection-by-eid coll-eid {:depth :snippet-meta})
          :children
          (find-where :name "chapter 1"))))))

;; Ownership
(expect
 identity ;; expect truthy
 (with-populated-db
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {folder-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                              (find-where :name "accidental astronauts")
                              :id
                              (data-project/project-by-eid {:depth :snippet-meta})
                              :collections
                              (find-where :name "accidental astronauts manuscript")
                              :children
                              (find-where :name "aa folder A"))]
     (some #{folder-eid} (data-user/owned-eids ryan-eid)))))

(expect
 identity ;; expect truthy
 (with-populated-db
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {folder-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                              (find-where :name "accidental astronauts")
                              :id
                              (data-project/project-by-eid {:depth :snippet-meta})
                              :collections
                              (find-where :name "accidental astronauts manuscript")
                              :children
                              (find-where :name "aa folder A")
                              :children
                              (find-where :name "aa folder AB"))]
     (some #{folder-eid} (data-user/owned-eids ryan-eid)))))

;; Update folders

(expect
 {:name "new folder name" :description "accidental astronauts folder A"}
 (in
  (with-populated-db
    (let [{ryan-eid :id} (data-user/user-for-username "ryan")
          folder (-> (data-project/projects-for-user-eid ryan-eid)
                     (find-where :name "accidental astronauts")
                     :id
                     (data-project/project-by-eid {:depth :snippet-meta})
                     :collections
                     (find-where :name "accidental astronauts manuscript")
                     :children
                     (find-where :name "aa folder A"))]
      (update-folder! (assoc folder :name "new folder name"))
      (folder-by-eid (:id folder))))))

(expect
 {:name "aa folder A" :description "new description"}
 (in
  (with-populated-db
    (let [{ryan-eid :id} (data-user/user-for-username "ryan")
          folder (-> (data-project/projects-for-user-eid ryan-eid)
                     (find-where :name "accidental astronauts")
                     :id
                     (data-project/project-by-eid {:depth :snippet-meta})
                     :collections
                     (find-where :name "accidental astronauts manuscript")
                     :children
                     (find-where :name "aa folder A"))]
      (update-folder! (assoc folder :description "new description"))
      (folder-by-eid (:id folder))))))
