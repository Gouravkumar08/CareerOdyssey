package com.outreach.dsa.service;

import com.outreach.dsa.dto.QuestionRequest;
import com.outreach.dsa.dto.SheetRequest;
import com.outreach.dsa.dto.SheetResponse;
import com.outreach.dsa.model.Difficulty;
import com.outreach.dsa.repository.SheetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * One-time data seed, in lieu of a Flyway migration (none exists in this project — schema is
 * Hibernate {@code ddl-auto: update}). Runs once at startup, guarded by
 * {@code sheetRepository.count() == 0}, so it's safe to leave in the codebase permanently: it
 * only ever seeds an empty DSA catalog, never touches sheets/questions/progress once any sheet
 * exists — including sheets created later by the user through the app itself.
 * <p>
 * Loads three reference sheets transcribed from user-provided source lists:
 * <ul>
 *     <li>"DSA Imp 170 Questions" — full topic/sub-topic/difficulty/link fidelity.</li>
 *     <li>"250+ DSA Questions for Placements" — difficulty taken from the source's Easy/Medium/Hard
 *     groupings; no links were present in the source (only "Link" placeholder text), so
 *     practiceLink is left blank for these to avoid fabricating URLs.</li>
 *     <li>"Placement DSA Questions" — the source has no difficulty column at all; every question
 *     defaults to Medium as an explicit placeholder, editable per-question afterward.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DsaSeedRunner implements ApplicationRunner {

    private final SheetRepository sheetRepository;
    private final SheetService sheetService;

    private static final Long SEED_USER_ID = 1L;

    @Override
    public void run(ApplicationArguments args) {
        if (sheetRepository.count() > 0) {
            log.info("DSA sheets already present — skipping seed.");
            return;
        }
        log.info("Seeding DSA reference sheets...");
        seedSheet("DSA Imp 170 Questions",
                "170-question curated sheet with topic, sub-topic, difficulty and practice link.",
                SHEET_170);
        seedSheet("250+ DSA Questions for Placements",
                "Broad placement-prep list grouped by topic and difficulty. Source had no problem links, so practice links are blank here — add them per-question as you go.",
                SHEET_250);
        seedSheet("Placement DSA Questions",
                "112-question placement list. Source had no difficulty column, so every question defaults to Medium — edit per-question to correct.",
                SHEET_112);
        log.info("DSA seed data loaded.");
    }

    private void seedSheet(String name, String description, String data) {
        SheetRequest sheetRequest = new SheetRequest();
        sheetRequest.setName(name);
        sheetRequest.setDescription(description);
        SheetResponse sheet = sheetService.create(sheetRequest);

        int count = 0;
        for (String line : data.strip().split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\\|", -1);
            QuestionRequest request = new QuestionRequest();
            request.setTopicName(parts[0].trim());
            request.setSubTopic(blankToNull(parts[1]));
            request.setTitle(parts[2].trim());
            request.setDifficulty(Difficulty.valueOf(parts[3].trim().toUpperCase()));
            String link = blankToNull(parts[4]);
            if (link != null && link.contains("youtube.com")) {
                request.setYoutubeLink(link);
            } else {
                request.setPracticeLink(link);
            }
            sheetService.addQuestion(sheet.getId(), request, SEED_USER_ID);
            count++;
        }
        log.info("Seeded sheet '{}' with {} question rows.", name, count);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // topic | subTopic | title | difficulty | link   (subTopic/link may be blank)

    private static final String SHEET_170 = """
            Array|Hashmap|Two Sum|EASY|https://leetcode.com/problems/two-sum/
            Array|Two-pointer/min-max|Best Time to Buy and Sell Stock|EASY|https://leetcode.com/problems/best-time-to-buy-and-sell-stock/
            Array|Two-pointer|Two Sum II (sorted input)|EASY|https://leetcode.com/problems/two-sum-ii-input-array-is-sorted/
            Array|Hashmap|Contains Duplicate|EASY|https://leetcode.com/problems/contains-duplicate/
            Array|Kadane's Algorithm|Maximum Subarray|EASY|https://leetcode.com/problems/maximum-subarray/
            Array|Two-pointer|3Sum|MEDIUM|https://leetcode.com/problems/3sum/
            Array|Logical|Product of Array Except Self|MEDIUM|https://leetcode.com/problems/product-of-array-except-self/
            Array|Two-pointer|Container With Most Water|MEDIUM|https://leetcode.com/problems/container-with-most-water/
            Array|Running sum|Subarray Sum Equals K|MEDIUM|https://leetcode.com/problems/subarray-sum-equals-k/
            Array|Running sum|Longest subarray with sum equals K|MEDIUM|https://practice.geeksforgeeks.org/problems/longest-sub-array-with-sum-k0809/1/
            Array|Running sum + pointer|Minimum Size Subarray Sum|MEDIUM|https://leetcode.com/problems/minimum-size-subarray-sum/
            Array|Hashmap|K-diff Pairs in an Array|MEDIUM|https://leetcode.com/problems/k-diff-pairs-in-an-array/
            Array||Maximum Length of Repeated Subarray|MEDIUM|https://leetcode.com/problems/maximum-length-of-repeated-subarray/
            Binary||Sum of Two Integers|EASY|https://leetcode.com/problems/sum-of-two-integers/
            Binary||Number of 1 Bits|EASY|https://leetcode.com/problems/number-of-1-bits/
            Binary||Counting Bits|MEDIUM|https://leetcode.com/problems/counting-bits/
            Binary||Missing Number|EASY|https://leetcode.com/problems/missing-number/
            Binary||Reverse Bits|EASY|https://leetcode.com/problems/reverse-bits/
            Array||Sort an array of 0's 1's 2's without using extra space or sorting algo|MEDIUM|https://leetcode.com/problems/sort-colors/
            Array||Repeat and Missing Number|MEDIUM|https://takeuforward.org/data-structure/find-the-repeating-and-missing-numbers/
            Array||Merge Overlapping Subintervals|MEDIUM|https://leetcode.com/problems/merge-intervals/
            Array||Find the duplicate in an array of N+1 integers|MEDIUM|https://leetcode.com/problems/find-the-duplicate-number/
            Array||Set Matrix Zeros|MEDIUM|https://leetcode.com/problems/set-matrix-zeroes/
            Array||Pascal Triangle|EASY|https://leetcode.com/problems/pascals-triangle/
            Array|Transpose of matrix|Rotate Matrix|MEDIUM|https://leetcode.com/problems/rotate-image/
            Array||Next Permutation|MEDIUM|https://leetcode.com/problems/next-permutation/
            Array||Search in a 2D matrix|MEDIUM|https://leetcode.com/problems/search-a-2d-matrix/
            Array|Merge-Sort|Inversion of Array|MEDIUM|https://www.geeksforgeeks.org/counting-inversions/
            Array|Merge-Sort|Reverse Pairs|HARD|https://leetcode.com/problems/reverse-pairs/
            Array|Sorting + 2-pointer|4 Sum|MEDIUM|https://leetcode.com/problems/4sum/
            Array||Majority Element (>N/2 times)|EASY|https://leetcode.com/problems/majority-element/
            Array||Majority Element (>N/3 times)|MEDIUM|https://leetcode.com/problems/majority-element-ii/
            Array||Longest Consecutive Sequence|MEDIUM|https://leetcode.com/problems/longest-consecutive-sequence/
            Array||Largest Subarray with 0 sum|MEDIUM|https://practice.geeksforgeeks.org/problems/largest-subarray-with-0-sum/
            Array||Count number of subarrays with given XOR|MEDIUM|https://bit.ly/3fp24yN
            Array|Kadane's Algorithm|Maximum Product Subarray|MEDIUM|https://leetcode.com/problems/maximum-product-subarray/
            Array||Partition to K Equal Sum Subsets|MEDIUM|https://leetcode.com/problems/partition-to-k-equal-sum-subsets/
            Array||Spiral Matrix|MEDIUM|https://leetcode.com/problems/spiral-matrix/
            Array|Two pointer|Trapping Rain Water|HARD|https://leetcode.com/problems/trapping-rain-water
            Array|Min Heap|K'th smallest element|EASY|https://practice.geeksforgeeks.org/problems/kth-smallest-element/0
            Array||First Missing Positive|HARD|https://leetcode.com/problems/first-missing-positive/
            Array||Top K Frequent Elements|MEDIUM|https://leetcode.com/problems/top-k-frequent-elements/
            Array||Verifying an Alien Dictionary|EASY|https://leetcode.com/problems/verifying-an-alien-dictionary/
            Array||Merge two sorted arrays|MEDIUM|https://practice.geeksforgeeks.org/problems/merge-two-sorted-arrays/0/
            Array||Equilibrium point|MEDIUM|https://practice.geeksforgeeks.org/problems/equilibrium-point/0
            Array||Element with left side smaller and right side greater|MEDIUM|https://practice.geeksforgeeks.org/problems/unsorted-array/0
            Array||Find Pivot Index|EASY|https://leetcode.com/problems/find-pivot-index/
            Binary Search||Median of Two Sorted Arrays|HARD|https://leetcode.com/problems/median-of-two-sorted-arrays/
            Binary Search||K-th element of two sorted arrays|MEDIUM|https://practice.geeksforgeeks.org/problems/k-th-element-of-two-sorted-array1317/1
            Binary Search||Search in a 2d Matrix|MEDIUM|https://leetcode.com/problems/search-a-2d-matrix/
            Binary Search||Find Minimum in Rotated Sorted Array|MEDIUM|https://leetcode.com/problems/find-minimum-in-rotated-sorted-array/
            Binary Search||Search element in a rotated sorted array|MEDIUM|https://leetcode.com/problems/search-in-rotated-sorted-array/
            String||Longest Palindromic Substring|MEDIUM|https://leetcode.com/problems/longest-palindromic-substring/
            String||Longest Common Prefix|EASY|https://leetcode.com/problems/longest-common-prefix/
            String|Divide and Conquer|Longest Nice Substring|EASY|https://leetcode.com/problems/longest-nice-substring
            String||Implement strStr()|EASY|https://leetcode.com/problems/implement-strstr/
            String|2-pointer + Hashmap|Find All Anagrams in a String|MEDIUM|https://leetcode.com/problems/find-all-anagrams-in-a-string/
            String|2-pointer + Hashmap|Permutation in String|MEDIUM|https://leetcode.com/problems/permutation-in-string/
            String|2-pointer + Hashmap|Longest Substring Without Repeating Characters|MEDIUM|https://leetcode.com/problems/longest-substring-without-repeating-characters/
            String|2-pointer + Hashmap|Minimum Window Substring|HARD|https://leetcode.com/problems/minimum-window-substring/
            String||Count and Say|MEDIUM|https://leetcode.com/problems/count-and-say/
            String||Compare Version Numbers|MEDIUM|https://leetcode.com/problems/compare-version-numbers/
            String|2-pointer + Hashmap|Substring with Concatenation of All Words|HARD|https://leetcode.com/problems/substring-with-concatenation-of-all-words/
            String||Longest Substring with At Least K Repeating Characters|MEDIUM|https://leetcode.com/problems/longest-substring-with-at-least-k-repeating-characters/
            String||Maximum Number of Non-Overlapping Substrings|HARD|https://leetcode.com/problems/maximum-number-of-non-overlapping-substrings/
            String||Minimum Insertions to Balance a Parentheses String|MEDIUM|https://leetcode.com/problems/minimum-insertions-to-balance-a-parentheses-string/
            String||Minimum Number of Swaps to Make the String Balanced|MEDIUM|https://leetcode.com/problems/minimum-number-of-swaps-to-make-the-string-balanced/
            String||Minimum Number of Steps to Make Two Strings Anagram|MEDIUM|https://leetcode.com/problems/minimum-number-of-steps-to-make-two-strings-anagram/
            Greedy||Best Time to Buy and Sell Stock II|MEDIUM|https://leetcode.com/problems/best-time-to-buy-and-sell-stock-ii/
            Greedy||Policemen catch thieves|MEDIUM|https://www.geeksforgeeks.org/policemen-catch-thieves/
            Greedy||Minimum Platforms|MEDIUM|https://practice.geeksforgeeks.org/problems/minimum-platforms/0
            Greedy||Jump Game|MEDIUM|https://leetcode.com/problems/jump-game/
            Greedy||Jump Game II|MEDIUM|https://leetcode.com/problems/jump-game-ii/
            Greedy||Gas Station|MEDIUM|https://leetcode.com/problems/gas-station/
            Greedy||N meetings in one room|MEDIUM|https://practice.geeksforgeeks.org/problems/n-meetings-in-one-room/0
            Recursion|Backtracking|Find all Subsets (subsequences)|MEDIUM|https://leetcode.com/problems/subsets/
            Recursion|Backtracking|Subsets with duplicate numbers in input|MEDIUM|https://leetcode.com/problems/subsets-ii/
            Recursion|Backtracking|Subset Sums|EASY|https://practice.geeksforgeeks.org/problems/subset-sums2234/1
            Recursion|Backtracking|Combination Sum|MEDIUM|https://leetcode.com/problems/combination-sum/
            Recursion|Backtracking|Combination Sum - II|MEDIUM|https://leetcode.com/problems/combination-sum-ii/
            Recursion|Backtracking|Combination Sum - III|MEDIUM|https://leetcode.com/problems/combination-sum-iii/
            Recursion|Backtracking|Partition to K Equal Sum Subsets|MEDIUM|https://leetcode.com/problems/partition-to-k-equal-sum-subsets/
            Recursion|Backtracking|Palindrome Partitioning|MEDIUM|https://leetcode.com/problems/palindrome-partitioning/
            Recursion||K-th permutation Sequence|HARD|https://leetcode.com/problems/permutation-sequence/
            Recursion|Backtracking|Print all permutations of a string/array|MEDIUM|https://leetcode.com/problems/permutations/
            Recursion|Backtracking|N-Queens|HARD|https://leetcode.com/problems/n-queens/
            Recursion|Backtracking|Sudoku Solver|HARD|https://leetcode.com/problems/sudoku-solver/
            Recursion|Backtracking|Rat in a Maze|MEDIUM|https://practice.geeksforgeeks.org/problems/rat-in-a-maze-problem/1
            Recursion|Backtracking|M coloring Problem|MEDIUM|https://practice.geeksforgeeks.org/problems/m-coloring-problem-1587115620/1
            Dynamic Programming||Coin Change|MEDIUM|https://leetcode.com/problems/coin-change/
            Dynamic Programming||House Robber|MEDIUM|https://leetcode.com/problems/house-robber/
            Dynamic Programming||Partition Equal Subset Sum|MEDIUM|https://leetcode.com/problems/partition-equal-subset-sum/
            Dynamic Programming||Climbing Stairs|EASY|https://leetcode.com/problems/climbing-stairs/
            Dynamic Programming||Minimum Path Sum|MEDIUM|https://leetcode.com/problems/minimum-path-sum/
            Dynamic Programming||Minimum Insertion Steps to Make a String Palindrome|HARD|https://leetcode.com/problems/minimum-insertion-steps-to-make-a-string-palindrome/
            Dynamic Programming||Longest Common Subsequence (LCS)|MEDIUM|https://leetcode.com/problems/longest-common-subsequence/
            Dynamic Programming||Minimum Insertions/Deletions to Convert String A to String B|MEDIUM|https://practice.geeksforgeeks.org/problems/minimum-number-of-deletions-and-insertions0209/1/
            Dynamic Programming||Longest Common Substring|MEDIUM|https://practice.geeksforgeeks.org/problems/longest-common-substring1452/1/
            Dynamic Programming||Longest Palindromic Subsequence|MEDIUM|https://leetcode.com/problems/longest-palindromic-subsequence/
            Dynamic Programming||Maximum Length of Repeated Subarray|MEDIUM|https://leetcode.com/problems/longest-common-subsequence/
            Dynamic Programming||0-1 Knapsack|MEDIUM|https://leetcode.com/problems/ones-and-zeroes/
            Dynamic Programming||Word Break|MEDIUM|https://leetcode.com/problems/word-break/
            Trees|Binary Tree|Iterative Inorder Traversal|MEDIUM|https://leetcode.com/problems/binary-tree-inorder-traversal/
            Trees|Binary Tree|Iterative Preorder Traversal|MEDIUM|https://leetcode.com/problems/binary-tree-preorder-traversal/
            Trees|Binary Tree|Iterative Postorder Traversal|MEDIUM|https://leetcode.com/problems/binary-tree-postorder-traversal/
            Trees|Binary Tree|Maximum Depth in Binary Tree|MEDIUM|https://leetcode.com/problems/maximum-depth-of-binary-tree/
            Trees|Binary Tree|Check if the Binary tree is height-balanced or not|MEDIUM|https://leetcode.com/problems/balanced-binary-tree/
            Trees|Binary Tree|Diameter of Binary Tree|MEDIUM|https://leetcode.com/problems/diameter-of-binary-tree/
            Trees|Binary Tree|Maximum path sum in Binary Tree|MEDIUM|https://leetcode.com/problems/binary-tree-maximum-path-sum/
            Trees|Binary Tree|Check if two trees are identical or not|MEDIUM|https://leetcode.com/problems/same-tree/
            Trees|Binary Tree|Zig Zag Traversal of Binary Tree|MEDIUM|https://leetcode.com/problems/binary-tree-zigzag-level-order-traversal/
            Trees|Binary Tree|Boundary Traversal of Binary Tree|MEDIUM|https://leetcode.com/problems/boundary-of-binary-tree/
            Trees|Binary Tree|Max width of a Binary Tree|MEDIUM|https://leetcode.com/problems/maximum-width-of-binary-tree/
            Trees|Binary Tree|Sum Root to Leaf Numbers|MEDIUM|https://leetcode.com/problems/sum-root-to-leaf-numbers/
            Trees|Binary Tree|LeftView Of Binary Tree|MEDIUM|https://practice.geeksforgeeks.org/problems/left-view-of-binary-tree/1
            Trees|Binary Tree|RightView Of Binary Tree|MEDIUM|https://leetcode.com/problems/binary-tree-right-side-view/
            Trees|Binary Tree|Vertical order traversal|MEDIUM|https://leetcode.com/problems/vertical-order-traversal-of-a-binary-tree/
            Trees|Binary Tree|Bottom View of Binary Tree|MEDIUM|https://practice.geeksforgeeks.org/problems/bottom-view-of-binary-tree/1
            Trees|Binary Tree|Top View of Binary Tree|MEDIUM|https://practice.geeksforgeeks.org/problems/top-view-of-binary-tree/1
            Trees|Binary Tree|Symmetric Binary Tree|MEDIUM|https://leetcode.com/problems/symmetric-tree/
            Trees|Binary Tree|Least Common Ancestor in Binary Tree|MEDIUM|https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-tree/
            Trees|Binary Tree|Populating Next Right Pointers in Each Node|MEDIUM|https://leetcode.com/problems/populating-next-right-pointers-in-each-node/
            Trees|Binary Tree|All Nodes Distance K in Binary Tree|MEDIUM|https://leetcode.com/problems/all-nodes-distance-k-in-binary-tree/
            Trees|Binary Tree|Minimum time taken to BURN the Binary Tree from a Node|MEDIUM|https://www.youtube.com/watch?v=2r5wLmQfD6g
            Trees|Binary Tree|Serialize and Deserialize a Binary Tree|MEDIUM|https://leetcode.com/problems/serialize-and-deserialize-binary-tree/
            Trees|Binary Tree|Flatten Binary Tree to LinkedList|MEDIUM|https://leetcode.com/problems/flatten-binary-tree-to-linked-list/
            Trees|Binary Search Tree|Find K-th smallest/largest element in BST|MEDIUM|https://leetcode.com/problems/kth-smallest-element-in-a-bst/
            Trees|Binary Search Tree|Check Tree is a BT is BST or not|MEDIUM|https://leetcode.com/problems/validate-binary-search-tree/
            Trees|Binary Search Tree|Find LCA of two nodes in BST|MEDIUM|https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-search-tree/
            Trees|Binary Search Tree|Two sum in BST|MEDIUM|https://leetcode.com/problems/two-sum-iv-input-is-a-bst/
            Trees|Binary Search Tree|BST iterator|MEDIUM|https://leetcode.com/problems/binary-search-tree-iterator/
            Trees|Binary Search Tree|Largest BST in a Binary Tree|MEDIUM|https://leetcode.com/problems/maximum-sum-bst-in-binary-tree/
            Graph|BFS|Word Ladder|MEDIUM|https://leetcode.com/problems/word-ladder/
            Graph|Topological Sort|Alien Dictionary|HARD|https://practice.geeksforgeeks.org/problems/alien-dictionary/1
            Graph|DFS|Number of Islands|MEDIUM|https://leetcode.com/problems/number-of-islands
            Graph|DFS|Find Word in Grid|MEDIUM|https://leetcode.com/problems/word-search
            Graph|BFS|Dijkstra's Algorithm (Shortest Path to every Vertex)|MEDIUM|https://practice.geeksforgeeks.org/problems/implementing-dijkstra-set-1-adjacency-matrix/1
            Graph|DFS|Flood Fill Algorithm|EASY|https://leetcode.com/problems/flood-fill/
            Graph|BFS|Rotting Oranges|MEDIUM|https://leetcode.com/problems/rotting-oranges
            Graph|BFS|Breadth First Search|MEDIUM|https://practice.geeksforgeeks.org/problems/bfs-traversal-of-graph/1
            Graph|DFS|Depth First Search|MEDIUM|https://practice.geeksforgeeks.org/problems/depth-first-traversal-for-a-graph/1
            Graph|BFS/DFS|Detect A cycle in Undirected Graph using BFS/DFS|MEDIUM|https://leetcode.com/problems/course-schedule/
            Graph|DFS|Detect A cycle in a Directed Graph using DFS|MEDIUM|https://www.youtube.com/watch?v=uzVUw90ZFIg
            Graph|BFS|Shortest Path in Directed Acyclic Graph (DAG)|MEDIUM|https://www.youtube.com/watch?v=CrxG4WJotgg
            Graph|DFS|Topological Sort (DFS)|MEDIUM|https://www.youtube.com/watch?v=Yh6EFazXipA
            Graph|DFS|Clone a Graph|MEDIUM|https://leetcode.com/problems/clone-graph/
            Graph|BFS/DFS|Number of Connected Components|MEDIUM|https://leetcode.com/problems/number-of-connected-components-in-an-undirected-graph/
            Linked List|Doubly Linked List|LRU Cache|MEDIUM|https://leetcode.com/problems/lru-cache
            Linked List|Doubly Linked List|First non-repeating character in a stream|MEDIUM|https://practice.geeksforgeeks.org/problems/first-non-repeating-character-in-a-stream/0
            Linked List||Reverse a LinkedList|EASY|https://leetcode.com/problems/reverse-linked-list/
            Linked List||Rotate a LinkedList|MEDIUM|https://leetcode.com/problems/rotate-list/
            Linked List||Find the middle of LinkedList|EASY|https://leetcode.com/problems/middle-of-the-linked-list
            Linked List||Reverse a Linked List in groups of size k|HARD|https://leetcode.com/problems/reverse-nodes-in-k-group/
            Linked List||Detect a cycle in Linked List|EASY|https://leetcode.com/problems/linked-list-cycle
            Linked List||Palindrome Linked List|EASY|https://leetcode.com/problems/palindrome-linked-list
            Linked List||Find the starting point of the Loop of LinkedList|MEDIUM|https://leetcode.com/problems/linked-list-cycle-ii/
            Linked List||Remove loop in Linked List|MEDIUM|https://practice.geeksforgeeks.org/problems/remove-loop-in-linked-list/1
            Linked List||Add two numbers as LinkedList|MEDIUM|https://leetcode.com/problems/add-two-numbers/
            Linked List||Merge two sorted Linked List|MEDIUM|https://leetcode.com/problems/merge-two-sorted-lists/
            Linked List||Flattening of a LinkedList|MEDIUM|https://practice.geeksforgeeks.org/problems/flattening-a-linked-list/1
            Stack & Queue||Check for balanced parentheses|EASY|https://leetcode.com/problems/valid-parentheses/
            Stack & Queue||Next Greater Element|EASY|https://leetcode.com/problems/next-greater-element-i/
            Stack & Queue||Implement Min Stack|EASY|https://leetcode.com/problems/min-stack/
            Stack & Queue||Sliding Window maximum|HARD|https://leetcode.com/problems/sliding-window-maximum/
            Stack & Queue||Largest rectangle in a histogram|HARD|https://leetcode.com/problems/largest-rectangle-in-histogram/
            Heap||Kth Largest Element|MEDIUM|https://leetcode.com/problems/kth-largest-element-in-an-array/
            Heap||Find Median from Data Stream|HARD|https://leetcode.com/problems/find-median-from-data-stream/
            Heap||Merge K sorted arrays|HARD|https://leetcode.com/problems/merge-k-sorted-lists/
            Heap||K most frequent elements|MEDIUM|https://leetcode.com/problems/top-k-frequent-elements/
            Heap||K Closest Points to Origin|MEDIUM|https://leetcode.com/problems/k-closest-points-to-origin/
            """;

    private static final String SHEET_250 = """
            Array||Two Sum|EASY|
            Array||Remove Duplicates from Sorted Array|EASY|
            Array||Best Time to Buy and Sell Stock|EASY|
            Array||Plus One|EASY|
            Array||Missing Number|EASY|
            Array||Maximum Subarray|EASY|
            Array||Move Zeroes|EASY|
            Array||Contains Duplicate|EASY|
            Array||Intersection of Two Arrays II|EASY|
            Array||Rotate Array|EASY|
            Array||Third Maximum Number|EASY|
            Array||Valid Palindrome|EASY|
            Array||Merge Sorted Array|EASY|
            Array||Maximum Product Subarray|EASY|
            Array||Minimum Size Subarray Sum|EASY|
            Array||Product of Array Except Self|MEDIUM|
            Array||Container With Most Water|MEDIUM|
            Array||Search in Rotated Sorted Array|MEDIUM|
            Array||Combination Sum|MEDIUM|
            Array||Next Permutation|MEDIUM|
            Array||Find First and Last Position of Element in Sorted Array|MEDIUM|
            Array||3Sum|MEDIUM|
            Array||Spiral Matrix|MEDIUM|
            Array||Merge Intervals|MEDIUM|
            Array||Jump Game|MEDIUM|
            Array||Set Matrix Zeroes|MEDIUM|
            Array||Group Anagrams|MEDIUM|
            Array||Word Search|MEDIUM|
            Array||First Missing Positive|MEDIUM|
            Array||Find Peak Element|MEDIUM|
            Array||Trapping Rain Water|HARD|
            Array||Best Time to Buy and Sell Stock III|HARD|
            Array||First Missing Positive|HARD|
            Array||Median of Two Sorted Arrays|HARD|
            Array||Jump Game II|HARD|
            Array||Longest Consecutive Sequence|HARD|
            Array||Minimum Window Substring|HARD|
            Array||Gas Station|HARD|
            Array||Meeting Rooms II|HARD|
            Array||Best Time to Buy and Sell Stock IV|HARD|
            Searching||First Bad Version|EASY|
            Searching||Search Insert Position|EASY|
            Searching||Guess Number Higher or Lower|EASY|
            Searching||Peak Index in a Mountain Array|EASY|
            Searching||Search a 2D Matrix II|EASY|
            Searching||Find Minimum in Rotated Sorted Array|EASY|
            Searching||Find K Closest Elements|EASY|
            Searching||First Missing Positive|EASY|
            Searching||Search in Rotated Sorted Array|EASY|
            Searching||Search a 2D Matrix|EASY|
            Searching||Divide Two Integers|MEDIUM|
            Searching||Median of Two Sorted Arrays|MEDIUM|
            Searching||Find Peak Element|MEDIUM|
            Searching||Find Kth Smallest Element in a Sorted Matrix|MEDIUM|
            Searching||Search in Rotated Sorted Array II|MEDIUM|
            Searching||Find First and Last Position of Element in Sorted Array|MEDIUM|
            Searching||First Missing Positive|MEDIUM|
            Searching||Search a 2D Matrix II|MEDIUM|
            Searching||Search Insert Position|MEDIUM|
            Searching||Search in Rotated Sorted Array|MEDIUM|
            Searching||Search in Rotated Sorted Array II|HARD|
            Searching||Median of Two Sorted Arrays|HARD|
            Searching||Search a 2D Matrix II|HARD|
            Searching||Find Minimum in Rotated Sorted Array|HARD|
            Searching||Search in Rotated Sorted Array|HARD|
            Searching||First Missing Positive|HARD|
            Searching||Search Insert Position|HARD|
            Searching||Find Peak Element|HARD|
            Searching||First Bad Version|HARD|
            Searching||Find First and Last Position of Element in Sorted Array|HARD|
            Recursion||Climbing Stairs|EASY|
            Recursion||Fibonacci Number|EASY|
            Recursion||Reverse String|EASY|
            Recursion||Pow(x, n)|EASY|
            Recursion||Merge Two Sorted Lists|EASY|
            Recursion||Maximum Depth of Binary Tree|EASY|
            Recursion||Symmetric Tree|EASY|
            Recursion||Path Sum|EASY|
            Recursion||Subsets|EASY|
            Recursion||Generate Parentheses|EASY|
            Recursion||Permutations|MEDIUM|
            Recursion||Combinations|MEDIUM|
            Recursion||Palindrome Partitioning|MEDIUM|
            Recursion||Expression Add Operators|MEDIUM|
            Recursion||Word Search|MEDIUM|
            Recursion||Unique Paths|MEDIUM|
            Recursion||Letter Combinations of a Phone Number|MEDIUM|
            Recursion||Generate Parentheses|MEDIUM|
            Recursion||Pow(x, n)|MEDIUM|
            Recursion||Reverse Linked List|MEDIUM|
            Recursion||Regular Expression Matching|HARD|
            Recursion||Palindrome Partitioning II|HARD|
            Recursion||Expression Add Operators|HARD|
            Recursion||Word Search II|HARD|
            Recursion||Wildcard Matching|HARD|
            Recursion||Unique Paths III|HARD|
            Recursion||Decode Ways|HARD|
            String||Reverse String|EASY|
            String||Valid Anagram|EASY|
            String||Valid Palindrome|EASY|
            String||String to Integer (atoi)|EASY|
            String||Implement strStr()|EASY|
            String||Count and Say|EASY|
            String||First Unique Character in a String|EASY|
            String||Valid Parentheses|EASY|
            String||Longest Substring Without Repeating Characters|EASY|
            String||Longest Common Prefix|EASY|
            String||Group Anagrams|MEDIUM|
            String||Longest Palindromic Substring|MEDIUM|
            String||ZigZag Conversion|MEDIUM|
            String||Regular Expression Matching|MEDIUM|
            String||Longest Valid Parentheses|MEDIUM|
            String||Implement strStr()|MEDIUM|
            String||String to Integer (atoi)|MEDIUM|
            String||Palindrome Partitioning|MEDIUM|
            String||Wildcard Matching|MEDIUM|
            String||Valid Parentheses|MEDIUM|
            String||Regular Expression Matching|HARD|
            String||Longest Substring with At Least K Repeating Characters|HARD|
            String||Distinct Subsequences|HARD|
            String||Longest Palindromic Substring|HARD|
            String||Encode and Decode Strings|HARD|
            String||Palindrome Partitioning II|HARD|
            String||Text Justification|HARD|
            String||Longest Valid Parentheses|HARD|
            String||Longest Common Prefix|HARD|
            String||Minimum Window Substring|HARD|
            Stack||Min Stack|EASY|
            Stack||Valid Parentheses|EASY|
            Stack||Remove Outermost Parentheses|EASY|
            Stack||Implement Queue using Stacks|EASY|
            Stack||Implement Stack using Queues|EASY|
            Stack||Remove K Digits|EASY|
            Stack||Baseball Game|EASY|
            Stack||Evaluate Reverse Polish Notation|MEDIUM|
            Stack||Basic Calculator II|MEDIUM|
            Stack||Daily Temperatures|MEDIUM|
            Stack||Trapping Rain Water|MEDIUM|
            Stack||Remove Duplicate Letters|MEDIUM|
            Stack||Valid Parentheses|MEDIUM|
            Stack||Longest Valid Parentheses|MEDIUM|
            Stack||Simplify Path|MEDIUM|
            Stack||Basic Calculator|MEDIUM|
            Stack||Largest Rectangle in Histogram|MEDIUM|
            Queue||Implement Stack using Queues|EASY|
            Queue||Design Circular Queue|EASY|
            Queue||Design Circular Deque|EASY|
            Queue||Implement Queue using Stacks|EASY|
            Queue||First Unique Character in a String|EASY|
            Queue||Design HashSet|EASY|
            Queue||Design HashMap|EASY|
            Queue||Implement Trie (Prefix Tree)|EASY|
            Queue||Sliding Window Maximum|EASY|
            Queue||Shortest Subarray with Sum at Least K|MEDIUM|
            Queue||Find K Pairs with Smallest Sums|MEDIUM|
            Queue||Design Twitter|MEDIUM|
            Queue||Number of Recent Calls|MEDIUM|
            Queue||Implement Stack using Queues|MEDIUM|
            Queue||Implement Queue using Stacks|MEDIUM|
            Queue||First Unique Number|MEDIUM|
            Queue||Sliding Window Median|MEDIUM|
            Queue||Task Scheduler|MEDIUM|
            Queue||Find the Most Competitive Subsequence|MEDIUM|
            Linked List||Reverse Linked List|EASY|
            Linked List||Middle of the Linked List|EASY|
            Linked List||Merge Two Sorted Lists|EASY|
            Linked List||Remove Nth Node From End of List|EASY|
            Linked List||Palindrome Linked List|EASY|
            Linked List||Intersection of Two Linked Lists|EASY|
            Linked List||Design Linked List|EASY|
            Linked List||Linked List Cycle|EASY|
            Linked List||Flatten a Multilevel Doubly Linked List|EASY|
            Linked List||Reverse Nodes in k-Group|EASY|
            Linked List||Add Two Numbers|MEDIUM|
            Linked List||Odd Even Linked List|MEDIUM|
            Linked List||Remove Linked List Elements|MEDIUM|
            Linked List||Copy List with Random Pointer|MEDIUM|
            Linked List||Swap Nodes in Pairs|MEDIUM|
            Linked List||Merge k Sorted Lists|MEDIUM|
            Linked List||LRU Cache|MEDIUM|
            Linked List||Palindrome Linked List|MEDIUM|
            Linked List||Design Linked List|MEDIUM|
            Linked List||Split Linked List in Parts|MEDIUM|
            Linked List||Reverse Nodes in k-Group|HARD|
            Linked List||LRUCache|HARD|
            Linked List||Serialize and Deserialize Binary Tree|HARD|
            Linked List||Merge k Sorted Lists|HARD|
            Linked List||Copy List with Random Pointer|HARD|
            Linked List||Merge Two Sorted Lists|HARD|
            Linked List||LRU Cache|HARD|
            Linked List||Palindrome Linked List|HARD|
            Linked List||Design Linked List|HARD|
            Tree||Maximum Depth of Binary Tree|EASY|
            Tree||Validate Binary Search Tree|EASY|
            Tree||Symmetric Tree|EASY|
            Tree||Convert Sorted Array to Binary Search Tree|EASY|
            Tree||Minimum Depth of Binary Tree|EASY|
            Tree||Diameter of Binary Tree|EASY|
            Tree||Same Tree|EASY|
            Tree||Subtree of Another Tree|EASY|
            Tree||Invert Binary Tree|EASY|
            Tree||Path Sum|EASY|
            Tree||Binary Tree Inorder Traversal|MEDIUM|
            Tree||Construct Binary Tree from Preorder and Inorder Traversal|MEDIUM|
            Tree||Construct Binary Tree from Inorder and Postorder Traversal|MEDIUM|
            Tree||Lowest Common Ancestor of a Binary Tree|MEDIUM|
            Tree||Binary Tree Level Order Traversal|MEDIUM|
            Tree||Validate Binary Search Tree|MEDIUM|
            Tree||Binary Tree Zigzag Level Order Traversal|MEDIUM|
            Tree||Path Sum II|MEDIUM|
            Tree||Count Complete Tree Nodes|MEDIUM|
            Tree||Flatten Binary Tree to Linked List|MEDIUM|
            Tree||Binary Tree Maximum Path Sum|HARD|
            Tree||Serialize and Deserialize Binary Tree|HARD|
            Tree||Construct Binary Tree from String|HARD|
            Tree||Populating Next Right Pointers in Each Node|HARD|
            Tree||Populating Next Right Pointers in Each Node II|HARD|
            Tree||House Robber III|HARD|
            Tree||Kth Smallest Element in a BST|HARD|
            Tree||Closest Binary Search Tree Value II|HARD|
            Tree||Convert Sorted List to Binary Search Tree|HARD|
            Tree||Maximum Binary Tree|HARD|
            Graph||Number of Islands|EASY|
            Graph||Valid Sudoku|EASY|
            Graph||Course Schedule|EASY|
            Graph||Is Graph Bipartite?|EASY|
            Graph||Friend Circles|EASY|
            Graph||Clone Graph|EASY|
            Graph||Pacific Atlantic Water Flow|EASY|
            Graph||Course Schedule II|EASY|
            Graph||Minimum Height Trees|EASY|
            Graph||Reconstruct Itinerary|EASY|
            Graph||Word Ladder|MEDIUM|
            Graph||Number of Connected Components in an Undirected Graph|MEDIUM|
            Graph||Graph Valid Tree|MEDIUM|
            Graph||Course Schedule III|MEDIUM|
            Graph||Alien Dictionary|MEDIUM|
            Graph||Network Delay Time|MEDIUM|
            Graph||Number of Islands II|MEDIUM|
            Graph||Pacific Atlantic Water Flow|MEDIUM|
            Graph||Reconstruct Itinerary|MEDIUM|
            Graph||Course Schedule|MEDIUM|
            Graph||Number of Islands III|HARD|
            Graph||Bus Routes|HARD|
            Graph||Critical Connections in a Network|HARD|
            Graph||Evaluate Division|HARD|
            Graph||Number of Restricted Paths From First to Last Node|HARD|
            Graph||Minimum Swaps to Group All 1's Together|HARD|
            Graph||Swim in Rising Water|HARD|
            Graph||Optimize Water Distribution in a Village|HARD|
            Graph||Regions Cut By Slashes|HARD|
            Graph||Most Stones Removed with Same Row or Column|HARD|
            """;

    private static final String SHEET_112 = """
            Arrays||Subarray with given sum|MEDIUM|
            Arrays||Count the triplets|MEDIUM|
            Arrays||Kadane's Algorithm|MEDIUM|
            Arrays||Missing number in array|MEDIUM|
            Arrays||Merge two sorted arrays|MEDIUM|
            Arrays||Rearrange array alternatively|MEDIUM|
            Arrays||Number of pairs|MEDIUM|
            Arrays||Inversion of Array|MEDIUM|
            Arrays||Sort an array of 0s, 1s and 2s|MEDIUM|
            Arrays||Equilibrium point|MEDIUM|
            Arrays||Leaders in an array|MEDIUM|
            Arrays||Minimum Platforms|MEDIUM|
            Arrays||Reverse array in groups|MEDIUM|
            Arrays||K'th smallest element|MEDIUM|
            Arrays||Trapping Rain Water|MEDIUM|
            Arrays||Pythagorean Triplet|MEDIUM|
            Arrays||Chocolate Distribution Problem|MEDIUM|
            Arrays||Stock buy and sell|MEDIUM|
            Arrays||Element with left side smaller and right side greater|MEDIUM|
            Arrays||Convert array into Zig-Zag fashion|MEDIUM|
            Arrays||Last Index of 1|MEDIUM|
            Arrays||Spirally traversing a matrix|MEDIUM|
            Arrays||Largest Number formed from an Array|MEDIUM|
            String||Reverse words in a given string|MEDIUM|
            String||Permutations of a given string|MEDIUM|
            String||Longest Palindrome in a String|MEDIUM|
            String||Recursively remove all adjacent duplicates|MEDIUM|
            String||Check if string is rotated by two places|MEDIUM|
            String||Roman Number to Integer|MEDIUM|
            String||Anagram|MEDIUM|
            String||Remove Duplicates|MEDIUM|
            String||Form a Palindrome|MEDIUM|
            String||Longest Distinct Characters in the string|MEDIUM|
            String||Implement Atoi|MEDIUM|
            String||Implement strstr|MEDIUM|
            String||Longest Common Prefix|MEDIUM|
            Linked List||Finding middle element in a linked list|MEDIUM|
            Linked List||Reverse a linked list|MEDIUM|
            Linked List||Rotate a Linked List|MEDIUM|
            Linked List||Reverse a Linked List in groups of given size|MEDIUM|
            Linked List||Intersection point in Y shaped linked lists|MEDIUM|
            Linked List||Detect Loop in linked list|MEDIUM|
            Linked List||Remove loop in Linked List|MEDIUM|
            Linked List||n'th node from end of linked list|MEDIUM|
            Linked List||Flattening a Linked List|MEDIUM|
            Linked List||Merge two sorted linked lists|MEDIUM|
            Linked List||Intersection point of two Linked Lists|MEDIUM|
            Linked List||Pairwise swap of a linked list|MEDIUM|
            Linked List||Add two numbers represented by linked lists|MEDIUM|
            Linked List||Check if Linked List is Palindrome|MEDIUM|
            Linked List||Implement Queue using Linked List|MEDIUM|
            Linked List||Implement Stack using Linked List|MEDIUM|
            Linked List||Given a linked list of 0s, 1s and 2s, sort it|MEDIUM|
            Linked List||Delete without head pointer|MEDIUM|
            Stack and Queue||Parenthesis Checker|MEDIUM|
            Stack and Queue||Next larger element|MEDIUM|
            Stack and Queue||Queue using two Stacks|MEDIUM|
            Stack and Queue||Stack using two queues|MEDIUM|
            Stack and Queue||Get minimum element from stack|MEDIUM|
            Stack and Queue||LRU Cache|MEDIUM|
            Stack and Queue||Circular tour|MEDIUM|
            Stack and Queue||First non-repeating character in a stream|MEDIUM|
            Stack and Queue||Rotten Oranges|MEDIUM|
            Stack and Queue||Maximum of all subarrays of size k|MEDIUM|
            Tree||Print Left View of Binary Tree|MEDIUM|
            Tree||Check for BST|MEDIUM|
            Tree||Print Bottom View of Binary Tree|MEDIUM|
            Tree||Print a Binary Tree in Vertical Order|MEDIUM|
            Tree||Level order traversal in spiral form|MEDIUM|
            Tree||Connect Nodes at Same Level|MEDIUM|
            Tree||Lowest Common Ancestor in a BST|MEDIUM|
            Tree||Convert a given Binary Tree to Doubly Linked List|MEDIUM|
            Tree||Write Code to Determine if Two Trees are Identical or Not|MEDIUM|
            Tree||Given a binary tree, check whether it is a mirror of itself|MEDIUM|
            Tree||Height of Binary Tree|MEDIUM|
            Tree||Maximum Path Sum|MEDIUM|
            Tree||Diameter of a Binary Tree|MEDIUM|
            Tree||Number of leaf nodes|MEDIUM|
            Tree||Check if given Binary Tree is Height Balanced or Not|MEDIUM|
            Tree||Serialize and Deserialize a Binary Tree|MEDIUM|
            Graph||Activity Selection|MEDIUM|
            Graph||N meetings in one room|MEDIUM|
            Graph||Coin Piles|MEDIUM|
            Graph||Maximize Toys|MEDIUM|
            Graph||Page Faults in LRU|MEDIUM|
            Graph||Largest number possible|MEDIUM|
            Graph||Minimize the heights|MEDIUM|
            Graph||Minimize the sum of product|MEDIUM|
            Graph||Huffman Decoding|MEDIUM|
            Graph||Minimum Spanning Tree|MEDIUM|
            Graph||Shop in Candy Store|MEDIUM|
            Graph||Geek collects the balls|MEDIUM|
            DP||Minimum Operations|MEDIUM|
            DP||Max length chain|MEDIUM|
            DP||Minimum number of Coins|MEDIUM|
            DP||Longest Common Substring|MEDIUM|
            DP||Longest Increasing Subsequence|MEDIUM|
            DP||Longest Common Subsequence|MEDIUM|
            DP||0-1 Knapsack Problem|MEDIUM|
            DP||Maximum sum increasing subsequence|MEDIUM|
            DP||Minimum number of jumps|MEDIUM|
            DP||Edit Distance|MEDIUM|
            DP||Coin Change Problem|MEDIUM|
            DP||Subset Sum Problem|MEDIUM|
            DP||Box Stacking|MEDIUM|
            DP||Rod Cutting|MEDIUM|
            DP||Path in Matrix|MEDIUM|
            DP||Minimum sum partition|MEDIUM|
            DP||Count number of ways to cover a distance|MEDIUM|
            DP||Egg Dropping Puzzle|MEDIUM|
            DP||Optimal Strategy for a Game|MEDIUM|
            DP||Shortest Common Supersequence|MEDIUM|
            """;
}
