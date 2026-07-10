(ns x402.directory
  "Vendor-neutral HTML directory-page renderer for any x402-compliant
  facilitator's `GET /catalog` response.

  x402 facilitators (gftdcojp/nexus-x402 among them) already serve a JSON
  `/catalog` for agents — a machine-readable menu of every gated resource
  (seller, price, gateway URL). This library renders that SAME data as a
  human-readable HTML page, so a facilitator operator doesn't have to write
  its own directory UI from scratch. Extracted (2026-07-10) from
  `gftdcojp/nexus-x402`'s `nexus.directory` (that facilitator's live,
  production directory page, generalized here into a reusable component —
  ADR-0002-style GTM step 3, but as an open library instead of one-off code).

  Pure string-building, zero deps, zero host interop — testable without a
  Workers/browser runtime, portable to any `.cljc` host (Cloudflare Workers,
  a plain HTTP server, a static-site generator).

  **Never fabricates.** Renders exactly what the caller's `:items` says —
  no placeholder stats, testimonials, or customer counts. An empty registry
  renders an honest empty state, not a lie. This constraint is deliberate
  and load-bearing: a directory page's only value is that it's true."
  (:require [clojure.string :as str]))

(defn- escape-html [s]
  (-> (str s)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")))

(defn- seller-row [{:keys [seller method path-prefix price description]}]
  (str "<tr>"
       "<td class=\"seller\">" (escape-html seller) "</td>"
       "<td><code>" (escape-html method) " " (escape-html path-prefix) "</code></td>"
       "<td class=\"price\">$" (escape-html (:usd price)) " " (escape-html (:asset price))
       "<span class=\"network\">" (escape-html (:network price)) "</span></td>"
       "<td>" (escape-html (or description "—")) "</td>"
       "</tr>"))

(defn- sellers-table [items empty-html]
  (if (seq items)
    (str "<table><thead><tr>"
         "<th>Seller</th><th>Gated resource</th><th>Price</th><th>Description</th>"
         "</tr></thead><tbody>"
         (str/join "" (map seller-row items))
         "</tbody></table>")
    empty-html))

(def ^:private default-page-css
  "html{color-scheme:light dark}
  body{font:16px/1.5 -apple-system,BlinkMacSystemFont,\"Segoe UI\",sans-serif;
       max-width:760px;margin:0 auto;padding:2.5rem 1.25rem;
       color:#1a1a1a;background:#fff}
  @media(prefers-color-scheme:dark){body{color:#e8e8e8;background:#111}}
  h1{font-size:1.75rem;margin-bottom:.25rem}
  .tagline{color:#666;margin-top:0}
  @media(prefers-color-scheme:dark){.tagline{color:#999}}
  .pitch{border-left:3px solid #0891b2;padding:.75rem 1rem;margin:1.5rem 0;
         background:rgba(8,145,178,.08);border-radius:0 6px 6px 0}
  table{width:100%;border-collapse:collapse;margin:1.5rem 0;font-size:.9rem}
  th,td{text-align:left;padding:.5rem .6rem;border-bottom:1px solid #e2e2e2}
  @media(prefers-color-scheme:dark){th,td{border-bottom:1px solid #333}}
  th{font-weight:600;color:#666}
  @media(prefers-color-scheme:dark){th{color:#999}}
  .seller{font-weight:600}
  .price{white-space:nowrap}
  .network{display:inline-block;margin-left:.4rem;font-size:.75rem;color:#666;
           background:#f0f0f0;border-radius:4px;padding:.05rem .4rem}
  @media(prefers-color-scheme:dark){.network{color:#aaa;background:#222}}
  code{font-size:.85em;background:#f0f0f0;border-radius:4px;padding:.1rem .35rem}
  @media(prefers-color-scheme:dark){code{background:#222}}
  .empty{color:#666}
  footer{margin-top:2.5rem;padding-top:1.25rem;border-top:1px solid #e2e2e2;
         font-size:.85rem;color:#666}
  @media(prefers-color-scheme:dark){footer{border-top:1px solid #333;color:#999}}
  a{color:#0891b2}
  .links a{margin-right:1rem}")

(def ^:private default-branding
  {:title "x402 facilitator"
   :tagline "Live, agent-native x402 payment facilitator."
   :pitch-html nil
   :empty-html "<p class=\"empty\">No sellers registered yet.</p>"
   :extra-links [{:href "/catalog" :label "Catalog (JSON)"}
                 {:href "/health" :label "Health"}]
   :css default-page-css})

(defn page
  "Render a full HTML directory page for an x402 facilitator's catalog.

   opts:
   - :origin   — the request origin, for the footer's absolute-link context.
   - :items    — /catalog's own :items (same shape, same source — never a
                 separate, divergeable dataset): a seq of
                 {:seller :method :path-prefix :price {:usd :asset :network}
                  :description}.
   - :branding — optional overrides merged over `default-branding`:
                 {:title :tagline :pitch-html :empty-html :extra-links :css}.
                 `:pitch-html` is raw HTML (the caller's own copy — this
                 library doesn't write marketing prose); pass nil to omit
                 the pitch block entirely."
  [{:keys [origin items branding]}]
  (let [{:keys [title tagline pitch-html empty-html extra-links css]}
        (merge default-branding branding)]
    (str
     "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
     "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
     "<title>" (escape-html title) "</title>"
     "<meta name=\"description\" content=\"" (escape-html tagline) "\">"
     "<style>" css "</style></head><body>"
     "<h1>" (escape-html title) "</h1>"
     "<p class=\"tagline\">" (escape-html tagline) "</p>"
     (when pitch-html (str "<div class=\"pitch\">" pitch-html "</div>"))
     "<h2>Gated resources (" (count items) ")</h2>"
     (sellers-table items empty-html)
     "<footer>"
     "<div class=\"links\">"
     (apply str (for [{:keys [href label]} extra-links]
                  (str "<a href=\"" (escape-html href) "\">" (escape-html label) "</a>")))
     "</div>"
     "<p>Origin: " (escape-html origin) "</p>"
     "</footer>"
     "</body></html>")))
