(ns pennydreadful.test.handler.collection
  (:require [expectations :refer :all]
            [ring.mock.request :refer :all]
            [clojure.edn :as edn]
            [clj-time.coerce :as tc]
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

;; PUT collection response
(expect
 {:status 201}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {coll-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                            (find-where :name "accidental astronauts")
                            :id
                            (data-project/project-by-eid {:depth :collection})
                            :collections
                            (find-where :name "accidental astronauts manuscript"))]
     (-> (request :put (str "/collection/" coll-eid))
         (body {:description "new description"})
         app)))))

;; PUT collection changes database
(expect
 {:name "accidental astronauts manuscript" :description "new description" :word-count-mode :manual :target 123456}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {coll-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                            (find-where :name "accidental astronauts")
                            :id
                            (data-project/project-by-eid {:depth :collection})
                            :collections
                            (find-where :name "accidental astronauts manuscript"))]
     (-> (request :put (str "/collection/" coll-eid))
         (body {:description "new description" :word-count-mode "manual" :target 123456})
         app)
     (data-coll/collection-by-eid coll-eid)))))

(expect
 {:name "accidental astronauts manuscript" :description "new description" :deadline-mode :manual :deadline (tc/from-date #inst "2014-11-29T00:00:00.000-00:00")}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {coll-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                            (find-where :name "accidental astronauts")
                            :id
                            (data-project/project-by-eid {:depth :collection})
                            :collections
                            (find-where :name "accidental astronauts manuscript"))]
     (-> (request :put (str "/collection/" coll-eid))
         (body {:description "new description" :deadline-mode "manual" :deadline "2014-11-29T00:00:00.000-00:00"})
         app)
     (data-coll/collection-by-eid coll-eid)))))

;; Cannot PUT to someone else's collection
(expect
 {:status 403}
 (in
  (login/as-ryan
   (let [{rhea-eid :id} (data-user/user-for-username "rhea")
         {coll-eid :id} (-> (data-project/projects-for-user-eid rhea-eid)
                            (find-where :name "condescending cougars")
                            :id
                            (data-project/project-by-eid {:depth :collection})
                            :collections
                            (find-where :name "condescending cougars manuscript"))]
     (-> (request :put (str "/collection/" coll-eid))
         (body {:name "malicious"})
         app)))))

(expect
 {:name "condescending cougars manuscript" :description "description here"}
 (in
  (login/as-ryan
   (let [{rhea-eid :id} (data-user/user-for-username "rhea")
         {coll-eid :id} (-> (data-project/projects-for-user-eid rhea-eid)
                            (find-where :name "condescending cougars")
                            :id
                            (data-project/project-by-eid {:depth :collection})
                            :collections
                            (find-where :name "condescending cougars manuscript"))]
     (-> (request :put (str "/collection/" coll-eid))
         (body {:name "malicious"})
         app)
     (data-coll/collection-by-eid coll-eid)))))

;; PUT ignores :id in collection map, instead uses id in URL
(expect
 {:name "accidental astronauts manuscript" :description "new description"}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         aa-proj (-> (data-project/projects-for-user-eid ryan-eid)
                     (find-where :name "accidental astronauts")
                     :id
                     (data-project/project-by-eid {:depth :collection}))
         {aam-coll-eid :id} (-> aa-proj
                                :collections
                                (find-where :name "accidental astronauts manuscript"))
         {aar-coll-eid :id} (-> aa-proj
                                :collections
                                (find-where :name "accidental astronauts research"))]
     (-> (request :put (str "/collection/" aam-coll-eid))
         (body {:id aar-coll-eid :description "new description"})
         app)
     (data-coll/collection-by-eid aam-coll-eid)))))

;; PUT /collection/:collection-eid/move-up response
(expect
 {:status 201}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {coll-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                            (find-where :name "accidental astronauts")
                            :id
                            (data-project/project-by-eid {:depth :collection})
                            :collections
                            (find-where :name "accidental astronauts research"))]
     (-> (request :put (str "/collection/" coll-eid "/move-up"))
         app)))))

;; PUT /collection/:collection-eid/move-down response
(expect
 {:status 201}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {coll-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                            (find-where :name "accidental astronauts")
                            :id
                            (data-project/project-by-eid {:depth :collection})
                            :collections
                            (find-where :name "accidental astronauts manuscript"))]
     (-> (request :put (str "/collection/" coll-eid "/move-down"))
         app)))))

;; Can't move-up someone else's collection
(expect
 {:status 401}
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
     (-> (request :put (str "/collection/" coll-eid "/move-up"))
         app)))))

;; Can't move-down someone else's collection
(expect
 {:status 401}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {rhea-eid :id} (data-user/user-for-username "rhea")
         {coll-eid :id} (-> (data-project/projects-for-user-eid rhea-eid)
                            (find-where :name "condescending cougars")
                            :id
                            (data-project/project-by-eid {:depth :collection})
                            :collections
                            (find-where :name "condescending cougars manuscript"))]
     (-> (request :put (str "/collection/" coll-eid "/move-down"))
         app)))))

;; PUT to /collection/:collection-eid/move-up mutates database correctly
(expect
 {:name "accidental astronauts research"}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {coll-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                            (find-where :name "accidental astronauts")
                            :id
                            (data-project/project-by-eid {:depth :collection})
                            :collections
                            (find-where :name "accidental astronauts research"))]
     (-> (request :put (str "/collection/" coll-eid "/move-up"))
         app)
     (-> (data-project/projects-for-user-eid ryan-eid)
         (find-where :name "accidental astronauts")
         :id
         (data-project/project-by-eid {:depth :collection})
         :collections
         (find-where :position 0))))))

;; PUT to /collection/:collection-eid/move-down mutates database correctly
(expect
 {:name "accidental astronauts research"}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {coll-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                            (find-where :name "accidental astronauts")
                            :id
                            (data-project/project-by-eid {:depth :collection})
                            :collections
                            (find-where :name "accidental astronauts manuscript"))]
     (-> (request :put (str "/collection/" coll-eid "/move-down"))
         app)
     (-> (data-project/projects-for-user-eid ryan-eid)
         (find-where :name "accidental astronauts")
         :id
         (data-project/project-by-eid {:depth :collection})
         :collections
         (find-where :position 0))))))
