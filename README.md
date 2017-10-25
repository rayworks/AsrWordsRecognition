AsrWordsRecognition
-------------------

This is a demo for testing sentences recognition with given `dictionary` and 
`language model`.

The following notes mainly taken and reedited from [tutorial concepts](https://cmusphinx.github.io/wiki/tutorialconcepts/).

# Background

The naive perception is often that speech is built with words and each word consists of phones. 
All modern descriptions of speech are to some degree probabilistic. That means that there are no 
certain boundaries between units, or between words.


## Structure of speech

Speech is a continuous audio stream where rather stable states mix with dynamically changed states. 
In this sequence of states, one can define more or less similar classes of sounds, or phones.

* Words are understood to be built of phones in general.
* transitions between words are more informative than stable regions
* The first part of the phone depends on its preceding phone, the middle part is stable and the next
 part depends on the subsequent phone. 
That’s why there are often three states in a phone selected for speech recognition.

For computational purpose it is helpful to detect parts of triphones instead of triphones as a whole.
Next, phones build subword units, like syllables. Sometimes, syllables are defined as 
“reduction-stable entities”. For instance, when speech becomes fast, phones often change, but 
syllables remain the same.Subwords form words. Words are important in speech recognition because 
they restrict combinations of phones significantly.


Words and other non-linguistic sounds, which we call fillers (breath, um, uh, cough), form utterances.

## Recognition process

The common way to recognize speech is the following: 
we take a waveform, split it at utterances by silences and then try to recognize what’s being said 
in each utterance. 
To do that, we want to take all possible combinations of words and try to match them with the audio.
We choose the best matching combination.


Matching process

### The concept of features

Since the number of parameters is large, we are trying to optimize it. Numbers that are calculated 
from speech usually by dividing the speech into frames. Then for each frame, typically of 10 
milliseconds length, we extract 39 numbers that represent the speech. That’s called a **feature vector**.
The way to generate the number of parameters is a subject of active investigation, but in a simple 
case it’s a derivative from the spectrum.

### Models

A model describes some mathematical object that gathers common attributes of the spoken word. It’s 
the most probable feature vector. From the concept of the model the following issues raise:

* how well does the model describe reality,
* can the model be made better of it’s internal model problems and
* how adaptive is the model if conditions change

The model of speech is called Hidden Markov Model or [HMM](https://en.wikipedia.org/wiki/Hidden_Markov_model) 
which proven to be really practical for speech decoding. 
It’s a generic model that describes a black-box communication channel.

### The matching process itself
Since it would take longer than universe existed to compare all feature vectors with all models, the
search is often optimized by applying many tricks. At any points we maintain the best matching 
variants and extend them as time goes on, producing the best matching variants for the next frame.

## Model Category
According to the speech structure, three models are used in speech recognition to do the match:

An acoustic model contains acoustic properties for each senone. There are context-independent models
 that contain properties 
(the most probable feature vectors for each phone) and context-dependent ones (built from senones 
with context).

A phonetic dictionary contains a mapping from words to phones. This mapping is not very effective. 
For example, only two to three pronunciation variants are noted in it. However, it’s practical 
enough most of the time. The dictionary is not the only method for mapping words to phones. You 
could also use some complex function learned with a machine learning algorithm.

A language model is used to restrict word search. It defines which word could follow previously 
recognized words (remember that matching is a sequential process) and helps to significantly 
restrict the matching process by stripping words that are not probable. The most common language 
models are n-gram language models–these contain statistics of word sequences–and finite state 
language models–these define speech sequences by finite state automation, sometimes with weights. To
reach a good accuracy rate, your language model must be very successful in search space restriction.
This means it should be very good at predicting the next word. A language model usually restricts 
the vocabulary that is considered to the words it contains. That’s an issue for name recognition. To
deal with this, a language model can contain smaller chunks like subwords or even phones. Please 
note that the search space restriction in this case is usually worse and the corresponding 
recognition accuracies are lower than with a word-based language model.

Those three entities are combined together in an engine to recognize speech. If you are going to 
apply your engine for some other language, you need to get such structures in place. For many 
languages there are acoustic models, phonetic dictionaries and even large vocabulary language models
available for download.

# Searches
As a developer you can configure several “search” objects with different grammars and language 
models and switch between them during runtime to provide interactive experience for the user.

There are multiple possible search modes:

* keyword: efficiently looks for a keyphrase and ignores other speech. It Allows to configure the 
detection threshold.
* grammar: recognizes speech according to the JSGF grammar. Unlike keyphrase search, grammar search 
doesn’t ignore words which are not in the grammar but tries to recognize them.
* ngram/lm: recognizes natural speech with a language model.
* allphone: recognizes phonemes with a phonetic language model.

Each search has a name and can be referenced by a name. Names are application-specific. The function
ps_set_search allows to activate the search that was previously added by a name.

In order to add a search, one needs to point to the grammar/language model describing the search. 
The location of the grammar is specific to the application. If only a simple recognition is required
it is sufficient to add a single search or to just configure the required mode using configuration 
options.

The exact design of a search depends on your application. For example, you might want to listen for 
an activation keyword first and once this keyword is recognized switch to ngram search to recognize 
the actual command. Once you recognized the command you can switch to grammar search to recognize 
the confirmation and then switch back to keyword listening mode to wait for another command.

 
 ##### Credit
 [CMUSphinx PocketSphinx - Recognize all (or large amount) of words](https://stackoverflow.com/questions/25949295/cmusphinx-pocketsphinx-recognize-all-or-large-amount-of-words/25951895#25951895)
 
 [CMUSphinx Tutorial For Developers](https://cmusphinx.github.io/wiki/tutorial/)
 
 [pocketsphinx android demo](https://github.com/cmusphinx/pocketsphinx-android-demo)