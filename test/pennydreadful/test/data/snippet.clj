(ns pennydreadful.test.data.snippet
  (:require [expectations :refer :all]
            [clj-time.coerce :as tc]
            [pennydreadful.test.util :refer :all]
            [pennydreadful.data.user :as data-user]
            [pennydreadful.data.project :as data-project]
            [pennydreadful.data.collection :as data-coll]
            [pennydreadful.data.folder :as data-folder]
            [pennydreadful.data.snippet :refer :all]))

;; Insert snippet into pennydreadful.data.collection
(expect
 {:name "scene 1" :description "a fell wind blows"}
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
