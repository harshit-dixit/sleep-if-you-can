package com.infusion.sleepifyoucan.data

import java.time.LocalDate

/**
 * Collection of motivational quotes for early risers.
 * Quotes rotate daily based on day of year for variety.
 */
object MotivationalQuotes {
    
    private val quotes = listOf(
        // Classic Motivation
        "The early morning has gold in its mouth. – Benjamin Franklin",
        "Wake up with determination. Go to bed with satisfaction.",
        "Every morning brings new potential, but if you dwell on the misfortunes of the day before, you tend to overlook tremendous opportunities.",
        "The way to get started is to quit talking and begin doing. – Walt Disney",
        "It's not about having time. It's about making time.",
        
        // Success & Achievement
        "Early to bed and early to rise makes a man healthy, wealthy, and wise. – Benjamin Franklin",
        "The difference between ordinary and extraordinary is that little extra.",
        "Success is not final, failure is not fatal: it is the courage to continue that counts. – Winston Churchill",
        "The only way to do great work is to love what you do. – Steve Jobs",
        "Your limitation—it's only your imagination.",
        
        // Morning Specific
        "Today is a new day. Don't let your history interfere with your destiny.",
        "Rise up, start fresh, see the bright opportunity in each new day.",
        "Morning is an important time of day, because how you spend your morning can often tell you what kind of day you are going to have.",
        "The breeze at dawn has secrets to tell you. Don't go back to sleep. – Rumi",
        "Every morning was a cheerful invitation to make life more simple. – Henry David Thoreau",
        
        // Energy & Action
        "Don't count the days, make the days count. – Muhammad Ali",
        "Today's actions are tomorrow's results.",
        "The secret of getting ahead is getting started. – Mark Twain",
        "Do what you have to do until you can do what you want to do. – Oprah Winfrey",
        "Dreams don't work unless you do.",
        
        // Mindset
        "Believe you can and you're halfway there. – Theodore Roosevelt",
        "The sun is a daily reminder that we too can rise again from the darkness.",
        "Some people dream of success, while other people get up every morning and make it happen.",
        "Your morning habits shape your future.",
        "Yesterday is history, tomorrow is a mystery, today is a gift.",
        
        // Persistence
        "It does not matter how slowly you go as long as you do not stop. – Confucius",
        "The harder you work for something, the greater you'll feel when you achieve it.",
        "Success is walking from failure to failure with no loss of enthusiasm. – Winston Churchill",
        "You don't have to be great to start, but you have to start to be great. – Zig Ziglar",
        "One day or day one. You decide.",
        
        // Fresh Start
        "With the new day comes new strength and new thoughts. – Eleanor Roosevelt",
        "Each morning we are born again. What we do today is what matters most. – Buddha",
        "A year from now you may wish you had started today. – Karen Lamb",
        "The best time to plant a tree was 20 years ago. The second best time is now.",
        "Make each day your masterpiece. – John Wooden",
        
        // Productivity
        "Lose an hour in the morning, and you will spend all day looking for it. – Richard Whately",
        "First thing every morning before you arise, say out loud 'I believe' three times. – Ovid",
        "The moment you feel like giving up, remember why you held on for so long.",
        "Great things never come from comfort zones.",
        "Either you run the day or the day runs you. – Jim Rohn",
        
        // Wellness
        "Take care of your body. It's the only place you have to live. – Jim Rohn",
        "Sleep is the best meditation. – Dalai Lama",
        "Happiness is not something ready made. It comes from your own actions. – Dalai Lama",
        "The groundwork for all happiness is good health. – Leigh Hunt",
        "Your body is a temple, but only if you treat it as one.",
        
        // Short & Punchy
        "Rise and grind. ☀️",
        "Today is your day!",
        "Wake up and be awesome.",
        "Make today count.",
        "New day, new possibilities.",
        "Champions wake up early.",
        "You've got this! 💪",
        "Early bird catches the worm. 🐛",
        "Mornings are for winners.",
        "Let's crush today!"
    )
    
    /**
     * Get the quote for today. Rotates daily based on day of year.
     */
    fun getQuoteOfTheDay(): String {
        val dayOfYear = LocalDate.now().dayOfYear
        val index = dayOfYear % quotes.size
        return quotes[index]
    }
    
    /**
     * Get a random quote from the collection.
     */
    fun getRandomQuote(): String {
        return quotes.random()
    }
    
    /**
     * Get all quotes.
     */
    fun getAllQuotes(): List<String> = quotes
}
