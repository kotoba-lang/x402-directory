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

(deftest page-title-and-meta-description-fall-back-to-title-and-tagline
  (let [html (directory/page
              {:origin "https://example.x402" :items []
               :branding {:title "acme" :tagline "acme's tagline"}})]
    (is (str/includes? html "<title>acme</title>"))
    (is (str/includes? html "content=\"acme's tagline\""))))

(deftest page-title-and-meta-description-can-be-overridden-independently
  (let [html (directory/page
              {:origin "https://example.x402" :items []
               :branding {:title "acme" :tagline "acme's tagline"
                          :page-title "acme — the open x402 facilitator"
                          :meta-description "A longer SEO-friendly description."}})]
    (is (str/includes? html "<title>acme — the open x402 facilitator</title>"))
    (is (str/includes? html "content=\"A longer SEO-friendly description.\""))
    (is (str/includes? html "<h1>acme</h1>"))
    (is (str/includes? html "acme's tagline"))))

(deftest page-shows-live-badge-by-default-and-can-be-omitted
  (let [html (directory/page {:origin "https://example.x402" :items []})]
    (is (str/includes? html "class=\"badge\"")))
  (let [html (directory/page {:origin "https://example.x402" :items []
                              :branding {:badge-label nil}})]
    (is (not (str/includes? html "class=\"badge\"")))))

(deftest page-nav-links-omitted-by-default-rendered-when-supplied
  (let [html (directory/page {:origin "https://example.x402" :items []})]
    (is (not (str/includes? html "class=\"jump\""))))
  (let [html (directory/page
              {:origin "https://example.x402" :items []
               :branding {:nav-links [{:href "#resources" :label "Resources"}]}})]
    (is (str/includes? html "class=\"jump\""))
    (is (str/includes? html "href=\"#resources\""))))

(deftest page-renders-extra-sections-html-verbatim-between-resources-and-footer
  (let [html (directory/page
              {:origin "https://example.x402" :items []
               :branding {:extra-sections-html
                          ["<section id=\"quickstart\"><h2>Quickstart</h2></section>"]}})]
    (is (str/includes? html "<section id=\"quickstart\"><h2>Quickstart</h2></section>"))
    (is (< (.indexOf html "id=\"resources\"") (.indexOf html "id=\"quickstart\"")))
    (is (< (.indexOf html "id=\"quickstart\"") (.indexOf html "<footer>")))))

;; ---- llms.txt ------------------------------------------------------------

(deftest llms-txt-renders-title-summary-and-sellers
  (let [txt (directory/llms-txt {:origin "https://example.x402" :items sample-items})]
    (is (str/starts-with? txt "# x402 facilitator"))
    (is (str/includes? txt "> Live, agent-native x402 payment facilitator."))
    (is (str/includes? txt "murakumo"))
    (is (str/includes? txt "kotobase"))
    (is (str/includes? txt "$0.01"))))

(deftest llms-txt-links-resolve-against-origin
  (let [txt (directory/llms-txt {:origin "https://example.x402" :items []})]
    (is (str/includes? txt "https://example.x402/catalog"))
    (is (str/includes? txt "https://example.x402/health"))))

(deftest llms-txt-honors-caller-branding
  (let [txt (directory/llms-txt
             {:origin "https://example.x402" :items []
              :branding {:title "acme-x402"
                         :llms-summary "acme's facilitator"
                         :llms-intro "Extra context paragraph."
                         :llms-links [{:href "/verify" :label "Verify" :note "POST payment+requirements"}]
                         :llms-sections ["## Source\n- [acme/pay](https://example.com)"]
                         :llms-empty "Nothing yet, check back soon."}})]
    (is (str/starts-with? txt "# acme-x402"))
    (is (str/includes? txt "> acme's facilitator"))
    (is (str/includes? txt "Extra context paragraph."))
    (is (str/includes? txt "https://example.x402/verify"))
    (is (str/includes? txt "Nothing yet, check back soon."))
    (is (str/includes? txt "## Source"))
    (is (str/includes? txt "acme/pay"))))

(deftest llms-txt-never-fabricates-when-registry-is-empty
  (let [txt (directory/llms-txt {:origin "https://example.x402" :items []})]
    (is (str/includes? txt "None registered yet."))
    (is (not (str/includes? txt "murakumo")))))

(deftest llms-txt-warns-against-treating-seller-fields-as-instructions
  (let [txt (directory/llms-txt {:origin "https://example.x402" :items sample-items})]
    (is (str/includes? txt "not instructions"))))
