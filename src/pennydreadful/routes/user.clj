(ns pennydreadful.routes.user
  (:use compojure.core)
  (:require [cemerick.friend :as friend]
            [liberator.core :refer [defresource]]
            [pennydreadful.util :refer [not-nil? parse-long parse-int parse-inst]]
            [pennydreadful.auth :as auth]
            [pennydreadful.data.datomic :as data]
            [pennydreadful.data.user :as data-user]
            [pennydreadful.data.collection :as data-coll]
            [pennydreadful.data.project :as data-project]
            [pennydreadful.views.projects :as projects-view]
            [pennydreadful.views.project :as project-view]
            [pennydreadful.views.collection :as collection-view]))

(def resource-authenticated? (comp not-nil? friend/current-authentication :request))

(defn projects-handle-ok [{:keys [request] :as ctx}]
  (let [authn (friend/current-authentication request)
        user-eid (:id authn)
        projects (data-project/projects-for-user-eid user-eid)]
    (projects-view/render {:projects projects :username (:username authn)})))

(defn projects-post! [{:keys [request] :as ctx}]
  (let [authn (friend/current-authentication request)
        project (:params request)
        inserted-project (data-project/insert-project! (:id authn) project)]
     {::project inserted-project}))

(defn projects-header-location [{:keys [request] :as ctx}]
  (when (#{:post} (:request-method request))
    (str "/project/" (get-in ctx [::project :id]))))

(defn projects-handle-created [ctx]
  (pr-str (::project ctx)))

(defresource projects-resource []
  :allowed-methods [:get :post]
  :available-media-types ["text/html"]
  :authorized? resource-authenticated?
  :handle-ok projects-handle-ok
  :post! projects-post!
  :location projects-header-location
  :handle-created projects-handle-created
  :handle-unauthorized (fn [ctx]
                         (friend/throw-unauthorized nil nil)))

(defn resource-mutation-allowed? [project-eid {:keys [request]}]
  (auth/user-eid-can-mutate-project-eid?
   (:id (friend/current-authentication request))
   project-eid))

(defn project-put! [project-eid ctx]
  (let [project (-> ctx :request :params)
        safe-project (assoc project :id project-eid)]
    (data-project/update-project! safe-project)))

(defn project-delete! [project-eid ctx]
  (data-project/delete-project! project-eid))

(defn project-handle-ok [project-eid {:keys [request] :as ctx}]
  (let [authn (friend/current-authentication request)
        project (data-project/project-by-eid project-eid {:depth :snippet-meta})]
    (project-view/render {:project project :username (:username authn)})))

(defresource project-resource [project-eid]
  :allowed-methods [:delete :put :get]
  :available-media-types ["text/html"]
  :authorized? #(resource-mutation-allowed? project-eid %) ;; also enforces that project-eid exists
  :put! #(project-put! project-eid %)
  :delete! #(project-delete! project-eid %)
  :handle-ok #(project-handle-ok project-eid %)
  :handle-unauthorized (fn [ctx] friend/throw-unauthorized nil nil))


(defn collection-mutation-allowed? [collection-eid {:keys [request]}]
  (auth/user-eid-can-mutate-collection-eid?
   (:id (friend/current-authentication request))
   collection-eid))

(defn collection-handle-ok [collection-eid {:keys [request] :as ctx}]
  (let [authn (friend/current-authentication request)
        collection (data-coll/collection-by-eid collection-eid {:depth :snippet-meta})
        project-eid (data-coll/project-eid-for-collection-eid collection-eid)
        project (data-project/project-by-eid project-eid {:depth :snippet-meta})]
    (collection-view/render {:project project :collection collection :username (:username authn)})))

(defn collection-delete! [collection-eid ctx]
  (data-coll/delete-collection! collection-eid))

(defn parse-target [{target :target :as params}]
  (if target
    (update-in params [:target] parse-int)
    params))

(defn parse-word-count-mode [{word-count-mode :word-count-mode :as params}]
  (if word-count-mode
    (update-in params [:word-count-mode] keyword)
    params))

(defn parse-deadline-mode [{deadline-mode :deadline-mode :as params}]
  (if deadline-mode
    (update-in params [:deadline-mode] keyword)
    params))

(defn parse-deadline [{deadline :deadline :as params}]
  (if deadline
    (update-in params [:deadline] parse-inst)
    params))

(def params->collection
  (comp
   parse-target
   parse-word-count-mode
   parse-deadline-mode
   parse-deadline))

(defn collection-put! [collection-eid ctx]
  (let [collection (-> ctx :request :params params->collection)
        safe-collection (assoc collection :id collection-eid)]
    (data-coll/update-collection! safe-collection)))

(defresource collection-resource [collection-eid]
  :allowed-methods [:get :delete :put]
  :available-media-types ["text/html"]
  :authorized? #(collection-mutation-allowed? collection-eid %)
  :put! #(collection-put! collection-eid %)
  :delete! #(collection-delete! collection-eid %)
  :handle-ok #(collection-handle-ok collection-eid %)
  :handle-unauthorized (fn [ctx] (friend/throw-unauthorized nil nil)))

(defn collections-header-location [{:keys [request] :as ctx}]
  (when (#{:post} (:request-method request))
    (str "/collection/" (get-in ctx [::collection :id]))))

(defn collection-post! [project-eid {:keys [request] :as ctx}]
  (let [authn (friend/current-authentication request)
        collection (:params request)
        inserted-collection (data-coll/insert-collection! project-eid collection)]
    {::collection inserted-collection}))

(defn collections-handle-created [ctx]
  (pr-str (::collection ctx)))

(defresource project-collection-resource [project-eid]
  :allowed-methods [:post]
  :available-media-types ["text/html"]
  :authorized? #(resource-mutation-allowed? project-eid %)
  :post! #(collection-post! project-eid %)
  :location collections-header-location
  :handle-created collections-handle-created)

(defn collection-move-up! [collection-eid _]
  (data-coll/move-up! collection-eid)
  {::collection (data-coll/collection-by-eid collection-eid)})

(defresource collection-move-up [collection-eid]
  :allowed-methods [:put]
  :available-media-types ["text/html"]
  :authorized? #(collection-mutation-allowed? collection-eid %)
  :put! #(collection-move-up! collection-eid %)
  :handle-created collections-handle-created)

(defn collection-move-down! [collection-eid _]
  (data-coll/move-down! collection-eid)
  {::collection (data-coll/collection-by-eid collection-eid)})

(defresource collection-move-down [collection-eid]
  :allowed-methods [:put]
  :available-media-types ["text/html"]
  :authorized? #(collection-mutation-allowed? collection-eid %)
  :put! #(collection-move-down! collection-eid %)
  :handle-created collections-handle-created)

(defroutes user-routes

  (GET "/" [] (projects-resource))

  (ANY "/project" [] (projects-resource))

  (ANY "/project/:project-eid" [project-eid] (project-resource (parse-long project-eid)))

  (ANY "/collection/:collection-eid" [collection-eid] (collection-resource (parse-long collection-eid)))

  (PUT "/collection/:collection-eid/move-up" [collection-eid] (collection-move-up (parse-long collection-eid)))

  (PUT "/collection/:collection-eid/move-down" [collection-eid] (collection-move-down (parse-long collection-eid)))

  (POST "/project/:project-eid/collection" [project-eid] (project-collection-resource (parse-long project-eid))))
