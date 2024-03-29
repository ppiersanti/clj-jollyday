(defproject clj-jollyday "0.1.1-alpha1"
  :description "Jollyday Clojure wrapper"
  :url "https://github.com/ppiersanti/clj-jollyday"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [de.jollyday/jollyday "0.5.2"]
                 [org.clojure/data.xml "0.2.0-alpha5"]
                 [org.clojure/data.zip "0.1.2"]]
  :deploy-repositories {"releases" {:url "https://repo.clojars.org" :creds :gpg}}
  :profiles {:test {:resource-paths ["test-resources"]}})


(cemerick.pomegranate.aether/register-wagon-factory!
 "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))
