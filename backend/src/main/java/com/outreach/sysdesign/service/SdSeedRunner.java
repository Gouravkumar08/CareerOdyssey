package com.outreach.sysdesign.service;

import com.outreach.sysdesign.dto.SdItemRequest;
import com.outreach.sysdesign.dto.SdSheetRequest;
import com.outreach.sysdesign.dto.SdSheetResponse;
import com.outreach.sysdesign.model.Category;
import com.outreach.sysdesign.repository.SdSheetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * One-time data seed, in lieu of a Flyway migration (none exists in this project — schema is
 * Hibernate {@code ddl-auto: update}). Runs once at startup, guarded by
 * {@code sdSheetRepository.count() == 0}, so it's safe to leave in the codebase permanently: it
 * only ever seeds an empty System Design catalog, never touches sheets/items/progress once any
 * sheet exists — including sheets created later by the user through the app itself.
 * <p>
 * Loads a single reference sheet, "HLD Roadmap", transcribed verbatim from a user-provided HLD
 * study document (topics, sub-topics and leaf concepts, with short prose descriptions synthesized
 * only where the source grouped several descriptive bullets under one concept heading). The
 * source is HLD-only, so every seeded item uses {@link Category#HLD}. No item is marked
 * "important". Resource links and notes come from {@link SdHldResourceCatalog} (a user-provided
 * curated guide); {@link SdHldResourceBackfillRunner} applies the same to already-seeded data. It only ever creates catalog rows (topics/items/sheet) — it never
 * creates or touches any {@code UserSdItemProgress} row, so every seeded item starts with zero
 * progress until the user marks it done themselves.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SdSeedRunner implements ApplicationRunner {

    private final SdSheetRepository sdSheetRepository;
    private final SdSheetService sdSheetService;

    private static final Long SEED_USER_ID = 1L;

    @Override
    public void run(ApplicationArguments args) {
        if (sdSheetRepository.count() > 0) {
            log.info("System Design sheets already present — skipping seed.");
            return;
        }
        log.info("Seeding System Design reference sheet...");

        SdSheetRequest sheetRequest = new SdSheetRequest();
        sheetRequest.setName("HLD Roadmap");
        sheetRequest.setDescription(
                "High-level design roadmap: core topics/subtopics, practical system designs, and key "
                        + "concepts mapped to non-functional requirements.");
        SdSheetResponse sheet = sdSheetService.create(sheetRequest);

        int count = 0;
        for (String line : ITEMS.strip().split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\\|", -1);
            SdItemRequest request = new SdItemRequest();
            request.setTopicName(parts[0].trim());
            request.setSubTopic(blankToNull(parts[1]));
            request.setTitle(parts[2].trim());
            request.setCategory(Category.HLD);
            request.setImportant(false);
            SdHldResourceCatalog.Resource resource =
                    SdHldResourceCatalog.resolve(request.getTopicName(), request.getSubTopic(), request.getTitle());
            request.setResourceLink(resource == null ? null : resource.link());
            request.setDescription(resource == null
                    ? blankToNull(parts[3])
                    : SdHldResourceCatalog.mergeNote(blankToNull(parts[3]), resource.note()));
            sdSheetService.addItem(sheet.getId(), request, SEED_USER_ID);
            count++;
        }
        log.info("Seeded sheet 'HLD Roadmap' with {} item rows.", count);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // topic | subTopic | title | description   (subTopic/description may be blank)

    private static final String ITEMS = """
            Background|Computer Architecture|Computer Architecture|
            Background|Application Architecture|Application Architecture|
            Background|Design Requirements|API Design|
            Background|Design Requirements|Database Schema|
            Background|Design Requirements|Read/Write Load|
            Background|Design Requirements|Social App|
            Background|Design Requirements|E-Commerce|
            Background|Design Requirements|Chat App|
            Background|Design Requirements|Read Heavy|
            Background|Design Requirements|Write Heavy|
            Background|Design Requirements|Number of Users|
            Background|Design Requirements|Data Per User|
            Background|Design Requirements|Ability to Handle Growth|
            Background|Design Requirements|Users|
            Background|Design Requirements|Requests|
            Background|Design Requirements|Horizontal Scaling|
            Background|Design Requirements|99%|
            Background|Design Requirements|99.9%|
            Background|Design Requirements|Replication|
            Background|Design Requirements|Load Balancing|
            Background|Design Requirements|Latency|API response under 100 ms.
            Background|Design Requirements|Throughput|
            Background|Design Requirements|Consistency|
            Background|Design Requirements|Durability|
            Background|Design Requirements|Fault Tolerance|
            Background|Design Requirements|Security|
            Background|Design Requirements|Observability|
            Background|Design Requirements|Rate Limiting|
            Networking|Networking Basics|Networking Fundamentals|
            Networking|Transport Layer|TCP|
            Networking|Transport Layer|UDP|
            Networking|DNS|DNS|
            Networking|Application Layer Protocols|Application Layer Protocols|
            APIs|API Design|API Design|
            APIs|HTTP|HTTP|
            APIs|WebSockets|WebSockets|
            APIs|API Paradigms|API Paradigms|
            APIs|Rate Limiting|Token Bucket|
            APIs|Rate Limiting|Leaky Bucket|
            APIs|Rate Limiting|Fixed Window Counter|
            APIs|Rate Limiting|Sliding Window Log|
            APIs|Rate Limiting|Sliding Window Counter|
            Storage|SQL|SQL Databases|
            Storage|NoSQL Databases|Key-Value Database|
            Storage|NoSQL Databases|Document Database|
            Storage|NoSQL Databases|Column-Wise / Wide-Column Database|
            Storage|NoSQL Databases|Graph Database|
            Storage|CAP Theorem|Consistency|
            Storage|CAP Theorem|Availability|
            Storage|CAP Theorem|Partition Tolerance|
            Storage|CAP Theorem|CAP Trade-offs|
            Storage|BASE|Basically Available|
            Storage|BASE|Soft State|
            Storage|BASE|Eventual Consistency|
            Storage|Replication|Keeping Copies of the Same Data|
            Storage|Replication|Synchronous Replication|Application writes to the primary, the primary writes to the replica, and success is only returned after the replica confirms. Provides strong consistency but higher latency.
            Storage|Replication|Asynchronous Replication|Application writes to the primary, which returns success immediately; the replica is updated later. Faster, but can cause replication lag, stale reads, and added failure-handling complexity. Common in use cases like social media feeds.
            Storage|Sharding|Splitting Data Across Multiple Databases / Servers|
            Storage|Sharding|Scale Writes|
            Storage|Sharding|Handle Huge Data|
            Storage|Sharding|Range-Based Sharding|Data divided by range (example: A-M to shard 1, N-Z to shard 2).
            Storage|Sharding|Hash-Based Sharding|Shard = Hash(User ID) % N.
            Storage|Sharding|Directory-Based Sharding|Maintain a mapping table (key to shard).
            Storage|Sharding|One Shard Can Become Overloaded|
            Storage|Sharding|Adding New Computers Can Be Difficult|
            Storage|Sharding|Rebalancing / Data Movement|
            Storage|Indexing|Faster Way to Find Data Without Scanning the Whole Table|
            Storage|Indexing|Index Stores Location of Data|
            Storage|Indexing|Common Structure: B-Tree|
            Storage|Indexing|Primary Index|Based on the primary key; unique.
            Storage|Indexing|Secondary Index|Based on other columns (example: name).
            Storage|Indexing|Composite Index|Multiple columns combined together.
            Storage|ACID|Atomicity|
            Storage|ACID|Consistency|
            Storage|ACID|Isolation|
            Storage|ACID|Durability|
            Caching & Proxies|Caching|Caching at Different Layers|
            Caching & Proxies|Caching|Client-Side Cache|Browser cache.
            Caching & Proxies|Caching|CDN|Content Delivery Network.
            Caching & Proxies|Caching|Load Balancer Cache|Cache at the load balancer layer.
            Caching & Proxies|Caching|Redis|
            Caching & Proxies|Caching|Other Server-Side Caches|
            Caching & Proxies|Caching Strategies|Cache-Aside|Request goes to the cache first; on a cache miss the data is read from the DB and written into the cache; subsequent reads are served from the cache.
            Caching & Proxies|Caching Strategies|Write-Through|Application writes to the cache, and the cache writes through to the DB. Higher consistency, but higher write latency.
            Caching & Proxies|Caching Strategies|Write-Back|Application writes to the cache, and the cache asynchronously writes to the DB. Faster writes, but possible data loss if the cache fails.
            Caching & Proxies|Caching Strategies|Read-Through|Application requests data from the cache; the cache handles the DB read on a miss and returns the data.
            Caching & Proxies|Caching Strategies|Write-Around|Application writes directly to the DB and the cache is not updated; avoids caching data that may not be read again.
            Caching & Proxies|Cache Invalidation|TTL|Data expires after a fixed time.
            Caching & Proxies|Cache Invalidation|Manual Invalidation|Explicitly invalidate/delete the cache entry.
            Caching & Proxies|Cache Invalidation|Cache-Control Headers|
            Caching & Proxies|Cache Invalidation|Invalidation|
            Caching & Proxies|Consistent Hashing|Used for Distributing Data Across Servers|
            Caching & Proxies|Consistent Hashing|Reduces Data Movement When Servers Are Added/Removed|
            Caching & Proxies|Consistent Hashing|Approximately 1/N of Total Keys Move When a Node Changes|
            Caching & Proxies|CDN Caching|TTL-Based|
            Caching & Proxies|CDN Caching|Cache-Control Headers|
            Caching & Proxies|CDN Caching|Cache Invalidation|
            Load Balancing|Load Balancing|Distribute Incoming Requests Across Multiple Servers|
            Load Balancing|Types|Layer 4 Load Balancer|Operates at the network layer, using TCP/UDP.
            Load Balancing|Types|Layer 7 Load Balancer|Operates at the application layer; handles HTTP, sessions, cookies, and request-level routing.
            Load Balancing|Static Load Balancing|Round Robin|Requests distributed in order.
            Load Balancing|Static Load Balancing|Weighted Round Robin|Servers receive requests according to capacity.
            Load Balancing|Static Load Balancing|IP Hash|Hash based on IP address.
            Load Balancing|Dynamic Load Balancing|Least Connections|Send the request to the server with the least active connections.
            Load Balancing|Dynamic Load Balancing|Weighted Least Connections|Consider server capacity along with active connections.
            Load Balancing|Dynamic Load Balancing|Least Response Time|Send the request to the server with the lowest response time.
            Load Balancing|Load Balancing Considerations|Server Capacity|
            Load Balancing|Load Balancing Considerations|Active Connections|
            Load Balancing|Load Balancing Considerations|Response Time|
            Load Balancing|Load Balancing Considerations|Health Checks|
            Load Balancing|Load Balancing Considerations|Session Handling|
            Big Data|Message Queues|Communicate Asynchronously Between Components|
            Big Data|Message Queues|Producer|
            Big Data|Message Queues|Queue|
            Big Data|Message Queues|Consumer|
            Big Data|Message Queues|At-Most-Once|Message delivered 0 or 1 time; no retry; possible message loss; no duplicate messages.
            Big Data|Message Queues|At-Least-Once|Message delivered 1 or more times; retries possible; possible duplicate messages.
            Big Data|Message Queues|Exactly-Once|Message delivered exactly once; no duplicate processing; more complex to implement.
            Big Data|Message Queue Patterns|Point-to-Point|One message, one consumer.
            Big Data|Message Queue Patterns|Publish-Subscribe|One message, multiple consumers.
            Big Data|Message Queue Use Cases|Background Processing|
            Big Data|Message Queue Use Cases|Event-Driven Systems|
            Big Data|Message Queue Use Cases|Traffic Spike Smoothing|
            Big Data|Message Queue Use Cases|Microservice Communication|
            Big Data|MapReduce|Map|
            Big Data|MapReduce|Reduce|
            Big Data|MapReduce|Distributed Data Processing|
            Scalability Components|Load Balancing|L4 Load Balancing|
            Scalability Components|Load Balancing|L7 Load Balancing|
            Scalability Components|Load Balancing|Static Algorithms|
            Scalability Components|Load Balancing|Dynamic Algorithms|
            Scalability Components|Proxies|Proxy Layer|
            Scalability Components|Proxies|Forward Proxy|
            Scalability Components|Proxies|Reverse Proxy|
            Scalability Components|Proxies|Request Routing|
            Scalability Components|Proxies|Caching|
            Scalability Components|Proxies|Security|
            Scalability Components|Consistent Hashing|Distribute Keys Across Nodes|
            Scalability Components|Consistent Hashing|Minimize Data Movement|
            Scalability Components|Consistent Hashing|Useful When Adding/Removing Servers|
            Scalability Components|Message Queues|Producer|
            Scalability Components|Message Queues|Queue|
            Scalability Components|Message Queues|Consumer|
            Scalability Components|Message Queues|Delivery Semantics|
            Scalability Components|Message Queues|Queue Patterns|
            URL Shortener|Practical Design|URL Generation|
            URL Shortener|Practical Design|Short Code Generation|
            URL Shortener|Practical Design|Database Design|
            URL Shortener|Practical Design|Caching|
            URL Shortener|Practical Design|Read/Write Scaling|
            URL Shortener|Practical Design|Load Balancing|
            Rate Limiter|Practical Design|Requirements|
            Rate Limiter|Practical Design|Token Bucket|
            Rate Limiter|Practical Design|Leaky Bucket|
            Rate Limiter|Practical Design|Fixed Window|
            Rate Limiter|Practical Design|Sliding Window Log|
            Rate Limiter|Practical Design|Sliding Window Counter|
            Rate Limiter|Practical Design|Distributed Rate Limiting|
            Rate Limiter|Practical Design|Redis|
            Rate Limiter|Practical Design|Scalability|
            Twitter|Practical Design|User Management|
            Twitter|Practical Design|Follow/Unfollow|
            Twitter|Practical Design|Tweet Creation|
            Twitter|Practical Design|Timeline / Feed|
            Twitter|Practical Design|Read-Heavy Architecture|
            Twitter|Practical Design|Caching|
            Twitter|Practical Design|Fan-Out|
            Twitter|Practical Design|Sharding|
            Twitter|Practical Design|Message Queues|
            Twitter|Practical Design|Load Balancing|
            WhatsApp|Practical Design|User Management|
            WhatsApp|Practical Design|One-to-One Messaging|
            WhatsApp|Practical Design|Group Messaging|
            WhatsApp|Practical Design|WebSockets|
            WhatsApp|Practical Design|Message Queues|
            WhatsApp|Practical Design|At-Most-Once|
            WhatsApp|Practical Design|At-Least-Once|
            WhatsApp|Practical Design|Exactly-Once|
            WhatsApp|Practical Design|Message Persistence|
            WhatsApp|Practical Design|Offline Messages|
            WhatsApp|Practical Design|Notifications|
            WhatsApp|Practical Design|Scalability|
            YouTube|Practical Design|Video Upload|
            YouTube|Practical Design|Video Storage|
            YouTube|Practical Design|Video Processing|
            YouTube|Practical Design|Video Transcoding|
            YouTube|Practical Design|CDN|
            YouTube|Practical Design|Caching|
            YouTube|Practical Design|Metadata Storage|
            YouTube|Practical Design|Object Storage|
            YouTube|Practical Design|Streaming|
            YouTube|Practical Design|Scalability|
            Key Concepts by Requirement|Read-Heavy System|Caching|
            Key Concepts by Requirement|Read-Heavy System|Read Replicas|
            Key Concepts by Requirement|Read-Heavy System|CDN|
            Key Concepts by Requirement|Read-Heavy System|Load Balancing|
            Key Concepts by Requirement|Write-Heavy System|Sharding|
            Key Concepts by Requirement|Write-Heavy System|Asynchronous Processing|
            Key Concepts by Requirement|Write-Heavy System|Message Queues|
            Key Concepts by Requirement|Write-Heavy System|Write-Optimized Storage|
            Key Concepts by Requirement|High Availability|Replication|
            Key Concepts by Requirement|High Availability|Load Balancing|
            Key Concepts by Requirement|High Availability|Failover|
            Key Concepts by Requirement|High Availability|Fault Tolerance|
            Key Concepts by Requirement|Low Latency|Caching|
            Key Concepts by Requirement|Low Latency|CDN|
            Key Concepts by Requirement|Low Latency|Reduce Network Calls|
            Key Concepts by Requirement|Low Latency|Read Replicas|
            Key Concepts by Requirement|Low Latency|In-Memory Storage|
            Key Concepts by Requirement|High Throughput|Horizontal Scaling|
            Key Concepts by Requirement|High Throughput|Multiple Servers|
            Key Concepts by Requirement|High Throughput|Asynchronous Processing|
            Key Concepts by Requirement|High Throughput|Message Queues|
            Key Concepts by Requirement|Eventual Consistency Acceptable|Asynchronous Replication|
            Key Concepts by Requirement|Eventual Consistency Acceptable|Message Queues|
            Key Concepts by Requirement|Eventual Consistency Acceptable|Event-Driven Architecture|
            Key Concepts by Requirement|Cache Invalidation|TTL|
            Key Concepts by Requirement|Cache Invalidation|Manual Invalidation|
            Key Concepts by Requirement|Cache Invalidation|Cache-Control Headers|
            Key Concepts by Requirement|Rate Limiting|Token Bucket|
            Key Concepts by Requirement|Rate Limiting|Leaky Bucket|
            Key Concepts by Requirement|Rate Limiting|Fixed Window Counter|
            Key Concepts by Requirement|Rate Limiting|Sliding Window Log|
            Key Concepts by Requirement|Rate Limiting|Sliding Window Counter|
            """;
}
