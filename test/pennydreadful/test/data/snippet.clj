(ns pennydreadful.test.data.snippet
  (:require [expectations :refer :all]
            [clj-time.coerce :as tc]
            [pennydreadful.test.util :refer :all]
            [pennydreadful.util :refer [chain-pprint]]
            [pennydreadful.data.user :as data-user]
            [pennydreadful.data.project :as data-project]
            [pennydreadful.data.collection :as data-coll]
            [pennydreadful.data.folder :as data-folder]
            [pennydreadful.data.snippet :refer :all]))

;; Insert snippet into pennydreadful.data.collection
(expect
 {:name "scene 1" :description "a fell wind blows" :position 2}
 (in
  (with-populated-db
    (let [{ryan-eid :id} (data-user/user-for-username "ryan")
          {coll-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                             (find-where :name "accidental astronauts")
                             :id
                             (data-project/project-by-eid {:depth :collection})
                             :collections
                             (find-where :name "accidental astronauts manuscript"))]
      (insert-snippet-into-collection! coll-eid
                                       {:name "scene 1"
                                        :description "a fell wind blows"})
      (-> (data-coll/collection-by-eid coll-eid {:depth :snippet-meta})
          :children
          (find-where :name "scene 1"))))))

;; Ownership
(expect
 identity ;; expect truthy
 (with-populated-db
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {snippet-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                               (find-where :name "accidental astronauts")
                               :id
                               (data-project/project-by-eid {:depth :snippet-meta})
                               :collections
                               (find-where :name "accidental astronauts manuscript")
                               :children
                               (find-where :name "aa snippet 1"))]
     (some #{snippet-eid} (data-user/owned-eids ryan-eid)))))

(expect
 not ;; expect falsey
 (with-populated-db
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {rhea-eid :id} (data-user/user-for-username "rhea")
         {snippet-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                               (find-where :name "accidental astronauts")
                               :id
                               (data-project/project-by-eid {:depth :snippet-meta})
                               :collections
                               (find-where :name "accidental astronauts manuscript")
                               :children
                               (find-where :name "aa snippet 1"))]
     (some #{snippet-eid} (data-user/owned-eids rhea-eid)))))

(expect
 identity ;; expect truthy
 (with-populated-db
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {snippet-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                               (find-where :name "accidental astronauts")
                               :id
                               (data-project/project-by-eid {:depth :snippet-meta})
                               :collections
                               (find-where :name "accidental astronauts research")
                               :children
                               (find-where :name "aa folder B")
                               :children
                               (find-where :name "aa folder BA")
                               :children
                               (find-where :name "aa snippet BA1"))]
     (some #{snippet-eid} (data-user/owned-eids ryan-eid)))))

(expect
 not ;; expect falsey
 (with-populated-db
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {rhea-eid :id} (data-user/user-for-username "rhea")
         {snippet-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                               (find-where :name "accidental astronauts")
                               :id
                               (data-project/project-by-eid {:depth :snippet-meta})
                               :collections
                               (find-where :name "accidental astronauts research")
                               :children
                               (find-where :name "aa folder B")
                               :children
                               (find-where :name "aa folder BA")
                               :children
                               (find-where :name "aa snippet BA1"))]
     (some #{snippet-eid} (data-user/owned-eids rhea-eid)))))

;; Update snippets

(expect
 {:name "new snippet name" :description "accidental astronauts snippet 1"}
 (in
  (with-populated-db
    (let [{ryan-eid :id} (data-user/user-for-username "ryan")
          snippet (-> (data-project/projects-for-user-eid ryan-eid)
                      (find-where :name "accidental astronauts")
                      :id
                      (data-project/project-by-eid {:depth :snippet-meta})
                      :collections
                      (find-where :name "accidental astronauts manuscript")
                      :children
                      (find-where :name "aa snippet 1"))]
      (update-snippet! (assoc snippet :name "new snippet name"))
      (snippet-by-eid (:id snippet))))))

(expect
 {:name "aa snippet 1" :description "new description"}
 (in
  (with-populated-db
    (let [{ryan-eid :id} (data-user/user-for-username "ryan")
          snippet (-> (data-project/projects-for-user-eid ryan-eid)
                      (find-where :name "accidental astronauts")
                      :id
                      (data-project/project-by-eid {:depth :snippet-meta})
                      :collections
                      (find-where :name "accidental astronauts manuscript")
                      :children
                      (find-where :name "aa snippet 1"))]
      (update-snippet! (assoc snippet :description "new description"))
      (snippet-by-eid (:id snippet))))))
