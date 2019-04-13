public class QuickSort {
  public  quickSort(ListNode head) {
    if (head == null || head.next == null){
      return head;
    }
    return quick(head, null);
  }

  private ListNode quick(ListNode start, ListNode end){
    if (start == null || start == end || start.next == end){
      return start;
    }

    ListNode[] result = partition(start, end);
    ListNode resultLeft = quick(result[0], result[1]);
    ListNode resultRight = quick(result[1].next, end);
    return resultLeft;
  }

  private ListNode[] partition(ListNode start, ListNode end){
    // start inclusive
    // end exclusive
    // return the new start node and the pivot node

    if (start == null || start == end || start.next == end){
      return new ListNode[] {start, start};
    }
    ListNode dummy = new ListNode(0);
    dummy.next = start;

    for (ListNode j = start; j != null && j.next != null && j.next != end; j = j.next) {
      while (j.next != null && j.next.value <= start.value){
        ListNode tmp = j.next;
        j.next = j.next.next;
        tmp.next = dummy.next;
        dummy.next = tmp;
      }
    }

    return new ListNode[] {dummy.next, start};
  }
}
