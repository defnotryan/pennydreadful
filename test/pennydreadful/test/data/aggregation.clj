(ns pennydreadful.test.data.aggregation
  (:require [expectations :refer :all]
            [pennydreadful.test.util :refer :all]
            [pennydreadful.data.aggregation :refer :all]
            [pennydreadful.data.collection :as data-coll]
            [pennydreadful.data.user :as data-user]
            [pennydreadful.data.project :as data-project]))

(expect
 4
 (with-populated-db
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         collection (-> (data-project/projects-for-user-eid ryan-eid)
                        (find-where :name "accidental astronauts")
                        :id
                        (data-project/project-by-eid {:depth :snippet})
                        :collections
                        (find-where :name "accidental astronauts manuscript"))]
     (word-count collection))))