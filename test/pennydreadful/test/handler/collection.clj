(ns pennydreadful.test.handler.collection
  (:require [expectations :refer :all]
            [ring.mock.request :refer :all]
            [clojure.edn :as edn]
            [pennydreadful.handler :refer [app]]
            [pennydreadful.test.util :refer :all]
            [pennydreadful.data.user :as data-user]
            [pennydreadful.data.project :as data-project]
            [pennydreadful.data.collection :as data-coll]
            [pennydreadful.test.handler.login :as login]))

;; 200 for gets
(expect
 {:status 200}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {coll-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                            (find-where :name "accidental astronauts")
                            :id
                            (data-project/project-by-eid {:depth :collection})
                            :collections
                            (find-where :name "accidental astronauts research"))]
     (app (request :get (str "/collection/" coll-eid)))))))

;; DELETE collection
(expect
 {:status 204}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {coll-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                            (find-where :name "accidental astronauts")
                            :id
                            (data-project/project-by-eid {:depth :collection})
                            :collections
                            (find-where :name "accidental astronauts research"))]
     (app (request :delete (str "/collection/" coll-eid)))))))
