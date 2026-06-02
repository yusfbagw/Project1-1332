package apply;

import java.util.Iterator;
import java.util.Random;
import refactor.StaticEndlessLinkedList;
import implement.ArrayDeque;
import refactor.EndlessLinkedList;

public class Quackify implements StaticQuackify {

    private EndlessLinkedList<String> playlist;
    
    private ArrayDeque<PlayListOperations> undoStack;
    private ArrayDeque<PlayListOperations> redoStack;

    private boolean isPlaying;
    private Iterator<String> iterator;
    private String currentSong;

    private Random random;

    public Quackify() {
        this.playlist = new EndlessLinkedList<>();
        this.undoStack = new ArrayDeque<>();
        this.redoStack = new ArrayDeque<>();
        this.random = new Random();
    }
    public Quackify(Random random) {
        this();
        this.random = random;
    }
    
    private static class PlayListOperations {
        private String type;
        private String song;
        private int index;

        private PlayListOperations(String t, String s, int i) {
            this.type = t;
            this.song = s;
            this.index = i;
        }
    }

    @Override
    /**
     * Signifies a state change to start music playback.
     * <p>
     * This should start the playlist from the beginning, regardless of when it
     * was last stopped.
     *
     * @throws IllegalStateException if the playlist is empty, or already playing
     * @implSpec {@code O(1)} runtime
     */
    public void play() {
        //Error Checking
        if (playlist.size() == 0) {
            throw new IllegalStateException("The playlist is empty.");
        }
        if (isPlaying) {
            throw new IllegalStateException("The playlist is already playing.");
        }
        isPlaying = true;
        /*
            This returns the most recently added song and then increments the iterators internal current node so that when 
            nextSong is called later it calls iterator.next() and we play the second song and so on.
        */
        this.iterator = playlist.iterator();
        this.currentSong = iterator.next();
    }
    
    /**
     * Signifies a state change to stop music playback.
     *
     * @throws IllegalStateException if the playlist is empty or already stopped
     * @implSpec {@code O(1)} runtime
     */
    @Override
    public void stop() {
        if (playlist.size() == 0) {
            throw new IllegalStateException("The playlist is empty.");
        }
        if (isPlaying == false) {
            throw new IllegalStateException("The playlist is not playing.");
        }

        //This stops music playback by setting the iterator to null.
        this.iterator = null;   
        isPlaying = false;
    }

    /**
     * Gets the current playing status.
     *
     * @return {@code true} if music is playing, {@code false} otherwise
     * @implSpec {@code O(1)} runtime
     */
    @Override
    public boolean isPlaying() {
        if (isPlaying)
        {
            return true;
        } else {
            return false;
        }
    }

     /**
     * Returns a snapshot of the playlist.
     *
     * @return the playlist, as an in-order EndlessLinkedList
     * @implSpec {@code O(1)} runtime
     */
    @Override
    public StaticEndlessLinkedList<String> getPlaylist() {
        EndlessLinkedList<String> endlessList = this.playlist;
        if (endlessList.size() == 0) {
            return endlessList;
        } else {
            return endlessList;
        }
    }

    /**
     * Retrieves the size of the playlist.
     *
     * @return the number of songs in the playlist.
     */
    @Override
    public int size() {
        int size = this.playlist.size();
        return size;
    }

    /**
     * Retrieves the current song being played in the playlist.
     *
     * @return the current song playing in the playlist
     * @throws IllegalStateException if the playlist is in a stopped state
     * @implSpec {@code O(1)} runtime
     */
    @Override
    public String currentSong() {
        if (!isPlaying) {
            throw new IllegalStateException("The playlist is in a stopped state.");
        }
        return currentSong;
    }

    /**
     * Iterates to the next song to play in the playlist.
     *
     * @return the next song in the playlist
     * @throws IllegalStateException if the playlist is in a stopped state
     * @implSpec {@code O(1)} runtime
     */
    @Override
    public String nextSong() {
        if (!isPlaying) {
            throw new IllegalStateException("The playlist is in a stopped state.");
        }
        //Here we're just taking the current song and then setting it equal to the next one and the 
        //call to currentSong already fixes any issue we might have.
        this.currentSong = this.iterator.next();
        return this.currentSong;
    }

    /**
     * Adds a song to the top of the playlist.
     *
     * @param song the song to add to the playlist
     * @throws IllegalStateException if the playlist is in a playing state
     * @throws IllegalArgumentException if {@code song} is {@code null} or an empty string
     * @implSpec {@code O(1)} runtime
     */
    @Override
    public void addSong(String song) {
        //Error Checking   
        if (isPlaying) {
            throw new IllegalStateException("The playlist is playing.");
        }
        if (song == null || song.isEmpty()) {
            throw new IllegalArgumentException("The song is null or an empty string, please fix.");
        }
        this.playlist.addFirst(song);
        this.undoStack.addFirst(new PlayListOperations("add", song, 0));
        this.redoStack.clear();
    }

     /**
     * Removes the first occurrence of a song from the playlist by song.
     *
     * @param song the song to remove from the playlist
     * @throws IllegalStateException if the playlist is in a playing state
     * @throws IllegalArgumentException if {@code song} is {@code null}
     * @throws IllegalArgumentException if {@code song} is {@code null} or an empty string
     * @implSpec {@code O(n)} runtime
     */
    @Override
    public void removeSong(String song) {
        if (isPlaying) {
            throw new IllegalStateException("The playlist is playing.");
        }
        if (song == null || song.isEmpty()) {
            throw new IllegalArgumentException("The song is null or an empty string, please fix.");
        }
        int returnValue = this.playlist.removeValue(song);
        this.undoStack.addFirst(new PlayListOperations("remove", song, returnValue));
        this.redoStack.clear();
    }

    /**
     * Reverses the playlist.
     *
     * @throws IllegalStateException if the playlist is empty or playing music
     * @implSpec
     * <p> {@code O(n)} runtime
     * <p> {@code O(1)} auxiliary space
     */
    @Override
    public void reverse() {
       if (this.playlist.size() == 0) {
        throw new IllegalStateException("The playlist is empty.");
       }
       if (isPlaying) {
        throw new IllegalStateException("The playlist is playing music.");
       }
       this.playlist.reverse();
       this.undoStack.addFirst(new PlayListOperations("reverse", null, -1));
       this.redoStack.clear();
    }

    /**
     * Uses {@link Random#nextInt(int)} to iterate to a random song in the playlist.
     * Randomly calculate an index of a song to jump to, and then start playing that song.
     * Optionally, consider what optimization can lower the number of {@code .next()} calls.
     * <p>
     * Note that this is just a one-time randomization; calling {@link StaticQuackify#nextSong()}
     * after going to a random song should return the song that comes next after the song we jump to.
     * <p>
     * You should use the range [0, n) for generating your random index, using the Random
     * from the constructor. You must use Random.randInt(b) to pick a random number.
     * You must use the one-argument method, where b is the exclusive upper bound.
     *
     * @return the song that we randomly jump to
     * @throws IllegalStateException if the playlist is in a stopped state
     * @implSpec {@code O(n)} runtime
     */
    @Override
    public String randomSong() {
       //Error Checking
        if (!isPlaying) {
            throw new IllegalStateException("The playlist is stopped");
       }
       
       int b = this.playlist.size();
       int randomIndex = this.random.nextInt(b);

       this.iterator = playlist.iterator();
       for (int i = 0; i <= randomIndex; i++) {
            currentSong = this.iterator.next();
       }
        return currentSong;
    }

    //MUST USE ARRAYDEQUE FOR THIS.
    /**
     * Determines whether the playlist itself (i.e., the list of song names) is a
     * palindrome.
     * <p>
     * Compare song names directly to each other, as the comparison is case-sensitive.
     *
     * @return whether the playlist itself is a palindrome
     * @throws IllegalStateException if the playlist is empty
     * @implNote do not use an array directly. You must use your ArrayDeque.
     * @implSpec {@code O(n)} runtime
     */
    @Override
    public boolean isPalindrome() {
        if (this.playlist.size() == 0) {
            throw new IllegalStateException("The playlist is empty. Please populate.");
        }

        ArrayDeque<String> newArrayDequeTwo = new ArrayDeque<String>();
        Iterator<String> iterator = this.playlist.iterator();
        
        //makes the iterator point at the playlists head
        //this.iterator = playlist.iterator();

        //Adds all elements from the linked list to the deque.
        for (int i = 0; i < this.playlist.size(); i++) {
            newArrayDequeTwo.addLast(iterator.next());
        }

        
        while (newArrayDequeTwo.size() > 1) {
            //First pop the first and last elements 
            // look if they're equal if they're not return false. 
            //Otherwise return true. 
            if (!(newArrayDequeTwo.removeFirst().equals(newArrayDequeTwo.removeLast()))) {
                return false;
            }
        } 
        return true;
    }

    /**
     * Undoes the most recent structural change from the playlist.
     * <p>
     * Structural changes include those made in {@link StaticQuackify#addSong(String)}
     * and {@link StaticQuackify#removeSong(String)}.
     * <p>
     * The operations should include the index in mind such that it recovers the order
     * properly in addition to the elements' existence.
     *
     * @throws IllegalStateException if the playlist is in a playing state,
     *                               or if there are no operations to undo
     * @implSpec {@code O(n)} runtime
     */
    @Override
    public void undo() {
        if (isPlaying) {
            throw new  IllegalStateException("The playlist is playing stop it.");
        }
        if (undoStack.size() == 0) {
            throw new IllegalStateException("There's nothing to undo.");
        }
        
        PlayListOperations operation = undoStack.removeFirst();
        //undoing the add, remove, and just rereversing it.
        if (operation.type.equals("add")) {
            playlist.removeFirst();
        } 
        else if (operation.type.equals("remove")) {
            //Create an arraydeque to hold temp values and find the first thing in the playlist and hold it here
            // if you need it again.
            ArrayDeque<String> dequeiestdequetemp = new ArrayDeque<String>();
            for (int i = 0; i < operation.index; i++) {
                dequeiestdequetemp.addLast(playlist.removeFirst());
            }

            playlist.addFirst(operation.song);
            while(dequeiestdequetemp.size() > 0){
                playlist.addFirst(dequeiestdequetemp.removeLast());
            }
        }
        else if (operation.type.equals("reverse")) {
            playlist.reverse();
        }
        redoStack.addFirst(operation);
    }

     /**
     * Redoes the most recent structural change undone from the playlist.
     * <p>
     * Structural changes include those made in {@link StaticQuackify#addSong(String)}
     * and {@link StaticQuackify#removeSong(String)}.
     * <p>
     * The operations should include the index in mind such that it recovers the order
     * properly in addition to the elements' existence. After an undo, if any new
     * structural modification occurs, the redo history should be cleared, since
     * redoing and undoing would no longer reconstruct the playlist correctly.
     *
     * @throws IllegalStateException if the playlist is in a playing state,
     *                               or if there are no operations to redo
     * @implSpec {@code O(n)} runtime
     */
    @Override
    public void redo() {
        if (isPlaying) {
            throw new  IllegalStateException("The playlist is playing stop it.");
        }
        if (redoStack.size() == 0) {
            throw new IllegalStateException("There's nothing to redo.");
        }
        //Redo stack pops from redoStack and pushes to undoStack.
        PlayListOperations operation = redoStack.removeFirst();
        if (operation.type.equals("add")) {
            playlist.addFirst(operation.song);
        } 
        else if (operation.type.equals("remove")) {
            playlist.removeValue(operation.song);
        }
        else if (operation.type.equals("reverse")) { 
            playlist.reverse();
        }
        undoStack.addFirst(operation);
    }
}
