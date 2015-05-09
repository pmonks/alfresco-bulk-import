#!/bin/sh

if [ -z "$1" ]; then
  echo "WARNING: this script will fail in unpleasant ways on non-Unicode filesystems."
fi

echo "Creating test files with spaces and long names..."
echo "This file has space characters in its name." > "file with spaces in the name.txt"
echo "This file has an exceptionally long name (longer than 128 characters).  It is highly likely that anyone using Windows will be unable to check this file out of SVN because of that platform's path length limitations." > "file with a very very very very very very very very very very very very very very very very very very long name that is longer than 128 characters.txt"

echo "Creating test files with Unicode names..."

mkdir -p Unicode
echo "Mika Pauli Häkkinen ( pronunciation (help·info)) (born 28 September 1968 in Vantaa in the Helsinki metropolitan area) is a Finnish racing driver and two-time Formula One World Champion. Seven-time world champion and rival Michael Schumacher said Häkkinen is the man he respected the most during his Formula One career." > Unicode/Finnish_Mika_Häkkinen.txt
echo "Gérard Xavier Marcel Depardieu (French pronunciation: [ʒeʁaʁ dəpaʁdjø] ( listen); born 27 December 1948) is a French actor and film-maker. He has won a number of honours including a nomination for an Academy Award for the title role in Cyrano de Bergerac and the Golden Globe award for Best Actor in Green Card. In addition to a number of American awards, Depardieu is a Chevalier of the Légion d'honneur, Chevalier of the Ordre national du Mérite and has twice won the César Award for Best Actor." > Unicode/French_Gérard_Depardieu.txt
echo "Jürgen Klinsmann (born 30 July 1964 in Göppingen) is a German football manager and former player and is currently the coach of the United States national team. As a player, Klinsmann played for several prominent clubs in Europe and was part of the West German team that won the 1990 FIFA World Cup and the German one that won the 1996 UEFA European Championship. He was one of West Germany's/Germany's premier strikers during the 90s. Klinsmann scored in all six major international tournaments he participated, from Euro 1988 to 1998 World Cup." > Unicode/German_Jürgen_Klinsmann.txt
echo "Yehoram Gaon (Hebrew: יהורם גאון) (informally, Yoram Gaon) (born December 28, 1939) is a Jewish Israeli singer, actor, director, producer, TV and radio host, and public figure. He has also written and edited books on Israeli culture.[1]" > Unicode/Hebrew_יהורם_גאון.txt
echo "Koel Purie Rinchet (born 25 November 1978 in Delhi, India) is an Indian film actress who made her debut with Rahul Bose's directorial venture Everybody Says I'm Fine! in 2001[1] and later featured in Road to Ladakh starring alongside Irrfan Khan. She attended the Royal Academy of Dramatic Art.[2]" > Unicode/Hindi_कोयल_पुरी.txt
echo "Björk Guðmundsdóttir [ˈpjœr̥k ˈkvʏðmʏntsˌtoʊhtɪr] ( listen) (born 21 November 1965), known as Björk (pronounced /ˈbjɜrk/ byurk in English), is an Icelandic singer-songwriter, occasional actress, music composer and music producer, whose influential solo work includes seven solo albums and two original film soundtracks." > Unicode/Icelandic_Björk_Guðmundsdóttir.txt
echo "Takeshi Kitano (北野 武 Kitano Takeshi?, born January 18, 1947) is a Japanese filmmaker, comedian, singer, actor, tap dancer, film editor, presenter, screenwriter, author, poet, painter, and one-time video game designer who has received critical acclaim, both in his native Japan and abroad, for his highly idiosyncratic cinematic work. The famed Japanese film critic Nagaharu Yodogawa once dubbed him 'the true successor' to influential filmmaker Akira Kurosawa.[1] With the exception of his works as a film director, he is known almost exclusively by the name Beat Takeshi (ビートたけし Bīto Takeshi?). Since April 2005, he has been a professor at the Graduate School of Visual Arts, Tokyo University of the Arts. Kitano owns his own talent agency and production company, Office Kitano, which launched Tokyo Filmex in 2000." > Unicode/Japanese_北野_武.txt
echo "Madhuri Dixit (Marathi: माधुरी दीक्षित) (born Madhuri Shankar Dixit on 15 May 1967)[1] is an Indian film actress who has appeared in Hindi films. Throughout the late 1980s and 1990s, she established herself as one of Hindi cinema's leading actresses and most accomplished dancers.[2] She appeared in numerous commercially successful films was recognised for several of her performances. Dixit is often cited by the media as one of the best actresses in Bollywood.[3][4] Dixit has won five Filmfare Awards, four for Best Actress and one for Best Supporting Actress. She holds the record for the highest number of Best Actress nominations at the Filmfare, with 13. In 2008, she was awarded the Padma Shri, India's fourth-highest civilian award by the Government of India.[5]" > Unicode/Marathi_माधुरी_दीक्षित.txt
echo "Omar Khayyám (Persian: عمر خیام, Early New Persian. pronunciation /ˈoːmɒːɾ xæjˈjɒːm/, English pronunciation /ˈoʊmɑr kaɪˈjɑm/) (18 May[2] 1048–1131) was a Persian mathematician, astronomer, philosopher and poet. He also wrote treatises on mechanics, geography, and music.[3]" > Unicode/Persian_عمر_خیام.txt
mkdir -p Unicode/Chinese_孔子

echo "Creating test files with punctuation characters in their names..."
mkdir -p Punctuation/Valid
echo "This file has both bracket characters [ and ] in its name." > Punctuation/Valid/brackets[].txt
echo "This file has both parenthesis characters ( and ) in its name." > Punctuation/Valid/parentheses\(\).txt
echo "This file has the single quote ' character in its name." > Punctuation/Valid/quote\'character.txt
#mkdir -p Punctuation/Invalid
# echo "This file has the backslash \ character in its name.  It is probable that users on Windows systems will be unable to check this file out of SVN." > Punctuation/Invalid/back\\slash.txt
#echo "This file has the double quote \" character in its name." > Punctuation/Invalid/double\"quote\"character.txt
