# x402-directory

**Vendor-neutral HTML directory-page renderer for any [x402](https://blog.cloudflare.com/monetization-gateway/)-compliant facilitator's `GET /catalog` response, in pure Clojure/ClojureScript (`.cljc`).**

x402 facilitators already serve a JSON `/catalog` so autonomous agents can
discover what they can pay for (seller, price, gateway URL). This library
renders that same data as a human-readable HTML page — so a facilitator
operator doesn't have to hand-write a directory UI. Extracted 2026-07-10 from
[`gftdcojp/nexus-x402`](https://x402.nexus)'s production directory page
(that facilitator's own live demo) into a reusable, branding-agnostic
component, and adopted back into nexus-x402 as its dependency (dogfooded,
not just published).

```clojure
(require '[x402.directory :as directory])

(directory/page
 {:origin "https://your-facilitator.example"
  :items [{:seller "murakumo" :method "POST" :path-prefix "/v1/messages"
           :price {:usd "0.01" :asset "USDC" :network "base"}
           :description "LLM inference, per request"}]
  :branding {:title "your-facilitator"
             :tagline "Self-hosted x402 payment facilitator."
             :pitch-html "<strong>No waitlist. Live today.</strong>"
             :extra-links [{:href "/catalog" :label "Catalog (JSON)"}
                           {:href "/health" :label "Health"}]}})
;; => "<!doctype html>..." — a complete, self-contained HTML page string
```

## What's in scope / out of scope

- **In scope**: rendering `{:seller :method :path-prefix :price :description}`
  items (the shape any x402 `/catalog` already returns) as an HTML table,
  with caller-supplied branding (title/tagline/pitch/links/CSS) and an
  honest empty state.
- **Out of scope**: fetching `/catalog` itself (the host does that — a
  Cloudflare Worker, a static-site build step, whatever), payment
  verification, and the facilitator/gateway logic itself (see
  [`kotoba-lang/pay`](https://github.com/kotoba-lang/pay) and
  [`kotoba-lang/treasury`](https://github.com/kotoba-lang/treasury) for
  that layer).

## Never fabricates

Renders exactly what `:items` says — no placeholder stats, testimonials, or
customer counts. An empty registry renders an honest empty state, not a
lie. A directory page's only value is that it's true; this library treats
that as a hard constraint, not a style preference (see the test suite's
`page-never-fabricates-when-registry-is-empty`).

## Live demo

[`x402.nexus`](https://x402.nexus) — [gftdcojp/nexus-x402](https://github.com/gftdcojp/nexus-x402)'s
self-hosted facilitator — runs this library in production (content-negotiated:
browsers get this HTML, API/agent clients hitting the same URL without an
`Accept: text/html` header get the unchanged JSON `/catalog` pointer).

## Test

```bash
clojure -M:test          # 8 tests
clojure -M:lint           # clj-kondo
```

Apache-2.0.
