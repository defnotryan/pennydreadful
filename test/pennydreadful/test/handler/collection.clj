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

(defn- make-post [body-content uri]
  (body (request :post uri) body-content))

(defn- post-new [collection project-eid]
  (-> collection
      (make-post (str "/project/" project-eid "/collection"))
      app))

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

;; Cannot DELETE someone else's collection
(expect
 {:status 403}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {rhea-eid :id} (data-user/user-for-username "rhea")
         {coll-eid :id} (-> (data-project/projects-for-user-eid rhea-eid)
                            (find-where :name "condescending cougars")
                            :id
                            (data-project/project-by-eid {:depth :collection})
                            :collections
                            (find-where :name "condescending cougars research"))]
     (app (request :delete (str "/collection/" coll-eid)))))))

;; DELETE actually removes collection from the database
(expect
 {}
 (login/as-ryan
  (let [{ryan-eid :id} (data-user/user-for-username "ryan")
        {coll-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                           (find-where :name "accidental astronauts")
                           :id
                           (data-project/project-by-eid {:depth :collection})
                           :collections
                           (find-where :name "accidental astronauts manuscript"))]
    (app (request :delete (str "/collection/" coll-eid)))
    (-> (data-coll/collection-by-eid coll-eid)
        (select-keys [:name :description])))))

;; POST collection returns 201 Created
(expect
 {:status 201}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {proj-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                            (find-where :name "accidental astronauts"))]
     (post-new {:name "accidental astronaut archives"} proj-eid)))))

;; POST collection returns location of new collection
(expect
 #"/collection/[0-9]+"
 (login/as-ryan
  (let [{ryan-eid :id} (data-user/user-for-username "ryan")
        {proj-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                           (find-where :name "accidental astronauts"))
        response (post-new {:name "accidental astronaut archives"} proj-eid)]
    (get-in response [:headers "Location"]))))

;; POST collection actuall adds collection to database
(expect
 #{"accidental astronauts manuscript" "accidental astronauts research" "accidental astronauts archives"}
 (login/as-ryan
  (let [{ryan-eid :id} (data-user/user-for-username "ryan")
        {proj-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                           (find-where :name "accidental astronauts"))]
    (post-new {:name "accidental astronauts archives"} proj-eid)
    (let [collections (-> proj-eid
                          (data-project/project-by-eid {:depth :collection})
                          :collections)]
      (into #{} (map :name collections))))))
