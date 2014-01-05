(ns pennydreadful.routes.user
  (:use compojure.core)
  (:require [cemerick.friend :as friend]
            [liberator.core :refer [defresource]]
            [pennydreadful.util :refer [not-nil? parse-long]]
            [pennydreadful.auth :as auth]
            [pennydreadful.data.datomic :as data]
            [pennydreadful.data.user :as data-user]
            [pennydreadful.data.collection :as data-coll]
            [pennydreadful.data.project :as data-project]
            [pennydreadful.views.projects :as projects-view]
            [pennydreadful.views.project :as project-view]))

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
        project (data-project/project-by-eid project-eid {:depth :snippet-names})]
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
  "OK")

(defn collection-delete! [collection-eid ctx]
  (data-coll/delete-collection! collection-eid))

(defresource collection-resource [collection-eid]
  :allowed-methods [:get :delete]
  :available-media-types ["text/html"]
  :authorized? #(collection-mutation-allowed? collection-eid %)
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

(defroutes user-routes

  (GET "/" [] (projects-resource))

  (ANY "/project" [] (projects-resource))

  (ANY "/project/:project-eid" [project-eid] (project-resource (parse-long project-eid)))

  (ANY "/collection/:collection-eid" [collection-eid] (collection-resource (parse-long collection-eid)))

  (POST "/project/:project-eid/collection" [project-eid] (project-collection-resource (parse-long project-eid))))
