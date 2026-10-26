package dev.taihei.mihon

/**
 * The list a given name was drawn from. It is closed on purpose: it says which of a language's two
 * given-name lists, and which form of a gendered family name (Russian Иванов and Иванова), a
 * [[Person]] has. Where a culture has unisex names, such a name is on both lists.
 */
enum Gender:

  /** The female given names, and the female form of a gendered family name (Иванова). */
  case Female

  /** The male given names, and the male form of a gendered family name (Иванов). */
  case Male
