(ns pennydreadful.test.data.aggregation
  (:require [expectations :refer :all]
            [clojure.pprint :refer [pprint]]
            [clj-time.coerce :as tc]
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

(expect
 3000
 (with-populated-db
   (let [{rhea-eid :id} (data-user/user-for-username "rhea")
         collection (-> (data-project/projects-for-user-eid rhea-eid)
                        (find-where :name "dwindling dandies")
                        :id
                        (data-project/project-by-eid {:depth :snippet})
                        :collections
                        (find-where :name "dwindling dandies manuscript"))]
     (word-count-target-aggregated collection))))

(expect
 (tc/from-date #inst "2014-11-30T00:00:00.000-00:00")
 (with-populated-db
   (let [{rhea-eid :id} (data-user/user-for-username "rhea")
         collection (-> (data-project/projects-for-user-eid rhea-eid)
                        (find-where :name "dwindling dandies")
                        :id
                        (data-project/project-by-eid {:depth :snippet})
                        :collections
                        (find-where :name "dwindling dandies manuscript"))]
     (deadline-aggregated collection))))
