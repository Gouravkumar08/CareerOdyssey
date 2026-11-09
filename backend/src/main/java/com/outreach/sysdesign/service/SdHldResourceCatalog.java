package com.outreach.sysdesign.service;

/**
 * Maps every HLD Roadmap item (topic / sub-topic / title, as seeded by {@link SdSeedRunner}) to a
 * primary learning resource, transcribed from the user-provided "System Design — Curated Learning
 * Resources" guide. Where one resource covers a cluster of rows, it is deliberately reused across
 * them (as the guide states). Notes are the guide's "Covers" / "Why" lines.
 * <p>
 * Used by {@link SdSeedRunner} for fresh databases and {@link SdHldResourceBackfillRunner} for
 * databases that were seeded before links existed.
 */
final class SdHldResourceCatalog {

    record Resource(String link, String note) {}

    private SdHldResourceCatalog() {}

    private static final Resource GFG_FOUNDATIONS = new Resource(
            "https://www.geeksforgeeks.org/system-design/system-design-tutorial/",
            "GeeksforGeeks System Design Tutorial\nCovers: Requirements, capacity, scalability, latency/throughput, reliability.\n"
                    + "Why: Broad current tutorial spanning CAP, reliability, load balancing, caching, consistent hashing, queues and rate limiting.");
    private static final Resource MIT_ARCH = new Resource(
            "https://ocw.mit.edu/courses/6-004-computation-structures-spring-2017/",
            "MIT 6.004 Computation Structures\nCovers: Architecture, memory hierarchy, caches, performance.\n"
                    + "Why: Full architecture/digital-systems course with lecture videos, notes and exercises.");
    private static final Resource STANFORD_NET = new Resource(
            "https://web.stanford.edu/class/cs144/",
            "Stanford CS144 — Introduction to Computer Networking\nCovers: Networking fundamentals, TCP/UDP, DNS.\n"
                    + "Why: Current networking course with practical networking material.");
    private static final Resource MDN_HTTP = new Resource(
            "https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Overview",
            "MDN — HTTP Overview\nCovers: HTTP, headers, caching; then WebSockets.\nWhy: Authoritative HTTP fundamentals.");
    private static final Resource GOOGLE_API = new Resource(
            "https://docs.cloud.google.com/apis/design",
            "Google Cloud API Design Guide\nCovers: Resource-oriented APIs, naming, methods, contracts.\n"
                    + "Why: Living API design guide covering resource-oriented design, naming and standard methods.");
    private static final Resource SQLBOLT = new Resource(
            "https://sqlbolt.com/",
            "SQLBolt\nCovers: SQL fluency, schema, joins, transactions.\nWhy: Interactive SQL lessons and exercises.");
    private static final Resource MONGO_NOSQL = new Resource(
            "https://www.mongodb.com/resources/basics/databases/nosql-explained/data-modeling",
            "MongoDB — NoSQL Data Modeling\nCovers: Key-value/document/wide-column/graph choices.\n"
                    + "Why: NoSQL modeling reference covering document and key-value approaches and access-pattern-driven design.");
    private static final Resource SDP = new Resource(
            "https://github.com/donnemartin/system-design-primer",
            "System Design Primer\nCovers: Consistency, availability, partition tolerance, eventual consistency; "
                    + "producer/consumer, patterns, async processing.\n"
                    + "Why: Accessible treatment of distributed-system trade-offs, async processing, queues, task queues, backpressure and decoupling.");
    private static final Resource REPLICATION = new Resource(
            "https://www.youtube.com/watch?v=fsZA4K0E3kQ",
            "Database Replication Explained — Sync vs Async\nCovers: Sync/async, lag, read replicas.\n"
                    + "Why: Recent 2026 explanation of primary/replica replication, WAL/binlog, sync/async, lag and read-your-own-writes.");
    private static final Resource AWS_SHARDING = new Resource(
            "https://aws.amazon.com/what-is/database-sharding/",
            "AWS — What Is Database Sharding?\nCovers: Shard keys, distribution, rebalancing.\n"
                    + "Why: Clear explanation of shards, shard keys and shared-nothing architecture.");
    private static final Resource REDIS_CACHE = new Resource(
            "https://redis.io/blog/cache-layer-architecture-guide/",
            "Redis — Cache Layer Architecture\nCovers: Cache-aside, write-through, write-back, write-around.\n"
                    + "Why: Current guide covering write-through, write-behind/write-back and write-around.");
    private static final Resource MDN_CACHING = new Resource(
            "https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Caching",
            "MDN — HTTP Caching\nCovers: TTL, Cache-Control, invalidation, edge caching.\n"
                    + "Why: Browser/shared-cache behavior, freshness, validation and cache-control patterns.");
    private static final Resource GFG_HASHING = new Resource(
            "https://www.geeksforgeeks.org/system-design/consistent-hashing/",
            "GFG — Consistent Hashing\nCovers: Hash ring and minimal movement.\n"
                    + "Why: Hash ring, node changes and minimizing key remapping.");
    private static final Resource CLOUDFLARE_LB = new Resource(
            "https://www.cloudflare.com/en-in/learning/performance/types-of-load-balancing-algorithms/",
            "Cloudflare — Load Balancing Algorithms\nCovers: L4/L7, round robin, least connections, IP hash.\n"
                    + "Why: Round robin, weighted round robin, IP hash, least connections and response-time/resource-based algorithms.");
    private static final Resource CONFLUENT = new Resource(
            "https://docs.confluent.io/kafka/design/delivery-semantics.html",
            "Confluent — Kafka Message Delivery Guarantees\nCovers: At-most-once, at-least-once, exactly-once.\n"
                    + "Why: At-most-once, at-least-once and exactly-once semantics, producer/consumer behavior and idempotency.");
    private static final Resource MAPREDUCE = new Resource(
            "https://research.google/pubs/mapreduce-simplified-data-processing-on-large-clusters/",
            "Google Research — MapReduce\nCovers: Map, Reduce, partitioning, distributed execution.\n"
                    + "Why: Original MapReduce paper covering Map, Reduce, partitioning, scheduling and failures.");
    private static final Resource OWASP = new Resource(
            "https://owasp.org/projects/api-security-project",
            "OWASP API Security Top 10 (2023)\nCovers: API security; logs/metrics/traces.\n"
                    + "Why: API security risks and mitigations.");
    private static final Resource URL_SHORTENER = new Resource(
            "https://bytebytego.com/courses/system-design-interview/design-a-url-shortener",
            "ByteByteGo — Design a URL Shortener\nCovers: Synthesis design.\n"
                    + "Why: End-to-end URL-shortener design with requirements, ID generation, storage and scaling.");
    private static final Resource RATE_LIMIT = new Resource(
            "https://blog.bytebytego.com/p/rate-limiting-fundamentals",
            "ByteByteGo — Rate Limiting Fundamentals\nCovers: All five algorithms + distributed rate limiting.\n"
                    + "Why: Rate-limiting algorithms and production design considerations.");
    private static final Resource TWITTER = new Resource(
            "https://github.com/donnemartin/system-design-primer/blob/master/solutions/system_design/twitter/README.md",
            "System Design Primer — Twitter Timeline\nCovers: Feed, fan-out, cache, sharding, queues.\n"
                    + "Why: Worked feed/timeline design with caching, fan-out, sharding and queues.");
    private static final Resource WHATSAPP = new Resource(
            "https://github.com/donnemartin/system-design-primer",
            "System Design Primer — Chat / WhatsApp references\nCovers: Messaging, WebSockets, persistence, offline.\n"
                    + "Why: Chat-system and WhatsApp architecture references.");
    private static final Resource YOUTUBE = new Resource(
            "https://docs.aws.amazon.com/solutions/latest/video-on-demand-on-aws-foundation/architecture-overview.html",
            "AWS — Video on Demand Architecture\nCovers: Upload, storage, processing, transcoding, CDN.\n"
                    + "Why: Upload, object storage, transcoding, metadata, notifications and CDN delivery.");

    static Resource resolve(String topic, String subTopic, String title) {
        String t = topic == null ? "" : topic;
        String s = subTopic == null ? "" : subTopic;
        String i = title == null ? "" : title;
        return switch (t) {
            case "Background" -> background(s, i);
            case "Networking" -> s.equals("Application Layer Protocols") ? MDN_HTTP : STANFORD_NET;
            case "APIs" -> switch (s) {
                case "HTTP", "WebSockets" -> MDN_HTTP;
                case "Rate Limiting" -> RATE_LIMIT;
                default -> GOOGLE_API;
            };
            case "Storage" -> switch (s) {
                case "NoSQL Databases" -> MONGO_NOSQL;
                case "CAP Theorem", "BASE" -> SDP;
                case "Replication" -> REPLICATION;
                case "Sharding" -> AWS_SHARDING;
                default -> SQLBOLT; // SQL, Indexing, ACID
            };
            case "Caching & Proxies" -> switch (s) {
                case "Caching" -> i.equals("Client-Side Cache") || i.equals("CDN") ? MDN_CACHING : REDIS_CACHE;
                case "Caching Strategies" -> REDIS_CACHE;
                case "Consistent Hashing" -> GFG_HASHING;
                default -> MDN_CACHING; // Cache Invalidation, CDN Caching
            };
            case "Load Balancing" -> CLOUDFLARE_LB;
            case "Big Data" -> switch (s) {
                case "MapReduce" -> MAPREDUCE;
                default -> isDeliverySemantics(i) ? CONFLUENT : SDP;
            };
            case "Scalability Components" -> switch (s) {
                case "Load Balancing" -> CLOUDFLARE_LB;
                case "Consistent Hashing" -> GFG_HASHING;
                default -> i.equals("Delivery Semantics") ? CONFLUENT : SDP; // Proxies, Message Queues
            };
            case "URL Shortener" -> URL_SHORTENER;
            case "Rate Limiter" -> RATE_LIMIT;
            case "Twitter" -> TWITTER;
            case "WhatsApp" -> isDeliverySemantics(i) ? CONFLUENT : WHATSAPP;
            case "YouTube" -> YOUTUBE;
            case "Key Concepts by Requirement" -> keyConcepts(s, i);
            default -> null;
        };
    }

    private static Resource background(String subTopic, String title) {
        if (subTopic.equals("Computer Architecture")) return MIT_ARCH;
        if (subTopic.equals("Application Architecture")) return GFG_FOUNDATIONS;
        return switch (title) {
            case "Security", "Observability" -> OWASP;
            case "Replication" -> REPLICATION;
            case "Load Balancing" -> CLOUDFLARE_LB;
            case "Rate Limiting" -> RATE_LIMIT;
            default -> GFG_FOUNDATIONS;
        };
    }

    private static Resource keyConcepts(String subTopic, String title) {
        if (subTopic.equals("Cache Invalidation")) return MDN_CACHING;
        if (subTopic.equals("Rate Limiting")) return RATE_LIMIT;
        return switch (title) {
            case "Caching", "In-Memory Storage" -> REDIS_CACHE;
            case "Read Replicas", "Replication", "Asynchronous Replication" -> REPLICATION;
            case "CDN" -> MDN_CACHING;
            case "Load Balancing" -> CLOUDFLARE_LB;
            case "Sharding" -> AWS_SHARDING;
            case "Write-Optimized Storage" -> MONGO_NOSQL;
            case "Asynchronous Processing", "Message Queues", "Event-Driven Architecture" -> SDP;
            default -> GFG_FOUNDATIONS; // Failover, Fault Tolerance, Reduce Network Calls, Horizontal Scaling, Multiple Servers
        };
    }

    private static boolean isDeliverySemantics(String title) {
        return title.equals("At-Most-Once") || title.equals("At-Least-Once") || title.equals("Exactly-Once");
    }

    /** Appends the catalog note to any existing description, without duplicating it on re-runs. */
    static String mergeNote(String existing, String note) {
        if (existing == null || existing.isBlank()) return note;
        if (existing.contains(note)) return existing;
        return existing.trim() + "\n\n" + note;
    }
}
