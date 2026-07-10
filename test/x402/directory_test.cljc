(ns x402.directory-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [x402.directory :as directory]))

(def sample-items
  [{:seller "murakumo" :method "POST" :path-prefix "/v1/messages"
    :price {:usd "0.01" :asset "USDC" :network "base"} :description nil}
   {:seller "kotobase" :method "GET" :path-prefix "/ipfs/"
    :price {:usd "0.001" :asset "USDC" :network "base"} :description "IPFS reads"}])

(deftest page-renders-every-seller
  (let [html (directory/page {:origin "https://example.x402" :items sample-items})]
    (is (str/includes? html "murakumo"))
    (is (str/includes? html "kotobase"))
    (is (str/includes? html "$0.01"))
    (is (str/includes? html "IPFS reads"))
    (is (str/includes? html "<!doctype html>"))))

(deftest page-uses-default-branding-when-none-given
  (let [html (directory/page {:origin "https://example.x402" :items []})]
    (is (str/includes? html "x402 facilitator"))
    (is (str/includes? html "No sellers registered yet"))
    (is (not (str/includes? html "<table>")))))

(deftest page-honors-caller-branding
  (let [html (directory/page
              {:origin "https://example.x402"
               :items sample-items
               :branding {:title "acme-x402"
                          :tagline "acme's facilitator"
                          :pitch-html "<strong>Live today.</strong>"
                          :extra-links [{:href "/catalog" :label "Catalog"}
                                        {:href "/about" :label "About"}]}})]
    (is (str/includes? html "acme-x402"))
    (is (str/includes? html "acme's facilitator"))
    (is (str/includes? html "<strong>Live today.</strong>"))
    (is (str/includes? html "href=\"/about\""))))

(deftest page-omits-pitch-block-when-nil
  (let [html (directory/page {:origin "https://example.x402" :items []})]
    (is (not (str/includes? html "class=\"pitch\"")))))

(deftest page-never-fabricates-when-registry-is-empty
  (testing "no placeholder leads/customers/results -- an empty registry
            renders an honest empty state, not a lie"
    (let [html (directory/page {:origin "https://example.x402" :items []})]
      (is (str/includes? html "No sellers registered yet"))
      (is (not (str/includes? html "<table>"))))))

(deftest page-caller-can-override-empty-state
  (let [html (directory/page
              {:origin "https://example.x402" :items []
               :branding {:empty-html "<p class=\"empty\">Register a seller via PUT /admin/sellers/&lt;seller&gt;.</p>"}})]
    (is (str/includes? html "Register a seller via"))))

(deftest page-escapes-untrusted-seller-content
  (testing "seller/description strings may come from third-party self-
            registrations -- must be HTML-escaped, not interpolated raw"
    (let [html (directory/page
                {:origin "https://example.x402"
                 :items [{:seller "<script>alert(1)</script>" :method "GET"
                          :path-prefix "/x" :price {:usd "1" :asset "USDC" :network "base"}
                          :description "<img src=x onerror=alert(2)>"}]})]
      (is (not (str/includes? html "<script>alert(1)</script>")))
      (is (not (str/includes? html "<img src=x onerror=alert(2)>")))
      (is (str/includes? html "&lt;script&gt;")))))

(deftest page-escapes-branding-title-and-tagline
  (let [html (directory/page
              {:origin "https://example.x402" :items []
               :branding {:title "<b>evil</b>" :tagline "<i>also evil</i>"}})]
    (is (not (str/includes? html "<b>evil</b>")))
    (is (not (str/includes? html "<i>also evil</i>")))))
