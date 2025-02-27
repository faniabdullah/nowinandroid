/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.nowinandroid.interests

import app.cash.turbine.test
import com.google.samples.apps.nowinandroid.core.model.data.Author
import com.google.samples.apps.nowinandroid.core.model.data.FollowableAuthor
import com.google.samples.apps.nowinandroid.core.model.data.FollowableTopic
import com.google.samples.apps.nowinandroid.core.model.data.Topic
import com.google.samples.apps.nowinandroid.core.testing.repository.TestAuthorsRepository
import com.google.samples.apps.nowinandroid.core.testing.repository.TestTopicsRepository
import com.google.samples.apps.nowinandroid.core.testing.repository.TestUserDataRepository
import com.google.samples.apps.nowinandroid.core.testing.util.TestDispatcherRule
import com.google.samples.apps.nowinandroid.feature.interests.InterestsUiState
import com.google.samples.apps.nowinandroid.feature.interests.InterestsViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class InterestsViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val userDataRepository = TestUserDataRepository()
    private val authorsRepository = TestAuthorsRepository()
    private val topicsRepository = TestTopicsRepository()
    private lateinit var viewModel: InterestsViewModel

    @Before
    fun setup() {
        viewModel = InterestsViewModel(
            userDataRepository = userDataRepository,
            authorsRepository = authorsRepository,
            topicsRepository = topicsRepository,
        )
    }

    @Test
    fun uiState_whenInitialized_thenShowLoading() = runTest {
        viewModel.uiState.test {
            assertEquals(InterestsUiState.Loading, awaitItem())
        }
    }

    @Test
    fun uiState_whenFollowedTopicsAreLoading_thenShowLoading() = runTest {
        viewModel.uiState.test {
            assertEquals(InterestsUiState.Loading, awaitItem())
            userDataRepository.setFollowedAuthorIds(setOf("1"))
            userDataRepository.setFollowedTopicIds(emptySet())
        }
    }

    @Test
    fun uiState_whenFollowedAuthorsAreLoading_thenShowLoading() = runTest {
        viewModel.uiState.test {
            assertEquals(InterestsUiState.Loading, awaitItem())
            userDataRepository.setFollowedAuthorIds(emptySet())
            userDataRepository.setFollowedTopicIds(setOf("1"))
        }
    }

    @Test
    fun uiState_whenFollowingNewTopic_thenShowUpdatedTopics() = runTest {
        val toggleTopicId = testOutputTopics[1].topic.id
        viewModel.uiState
            .test {
                awaitItem()
                authorsRepository.sendAuthors(emptyList())
                userDataRepository.setFollowedAuthorIds(emptySet())
                topicsRepository.sendTopics(testInputTopics.map { it.topic })
                userDataRepository.setFollowedTopicIds(setOf(testInputTopics[0].topic.id))

                assertEquals(
                    false,
                    (awaitItem() as InterestsUiState.Interests)
                        .topics.first { it.topic.id == toggleTopicId }.isFollowed
                )

                viewModel.followTopic(
                    followedTopicId = toggleTopicId,
                    true
                )

                assertEquals(
                    InterestsUiState.Interests(topics = testOutputTopics, authors = emptyList()),
                    awaitItem()
                )
            }
    }

    @Test
    fun uiState_whenFollowingNewAuthor_thenShowUpdatedAuthors() = runTest {
        viewModel.uiState
            .test {
                awaitItem()
                authorsRepository.sendAuthors(testInputAuthors.map { it.author })
                userDataRepository.setFollowedAuthorIds(setOf(testInputAuthors[0].author.id))
                topicsRepository.sendTopics(listOf())
                userDataRepository.setFollowedTopicIds(setOf())

                awaitItem()
                viewModel.followAuthor(
                    followedAuthorId = testInputAuthors[1].author.id,
                    followed = true
                )

                assertEquals(
                    InterestsUiState.Interests(topics = emptyList(), authors = testOutputAuthors),
                    awaitItem()
                )
            }
    }

    @Test
    fun uiState_whenUnfollowingTopics_thenShowUpdatedTopics() = runTest {
        val toggleTopicId = testOutputTopics[1].topic.id
        viewModel.uiState
            .test {
                awaitItem()
                authorsRepository.sendAuthors(emptyList())
                userDataRepository.setFollowedAuthorIds(emptySet())
                topicsRepository.sendTopics(testOutputTopics.map { it.topic })
                userDataRepository.setFollowedTopicIds(
                    setOf(testOutputTopics[0].topic.id, testOutputTopics[1].topic.id)
                )

                assertEquals(
                    true,
                    (awaitItem() as InterestsUiState.Interests)
                        .topics.first { it.topic.id == toggleTopicId }.isFollowed
                )

                viewModel.followTopic(
                    followedTopicId = toggleTopicId,
                    false
                )

                assertEquals(
                    InterestsUiState.Interests(topics = testInputTopics, authors = emptyList()),
                    awaitItem()
                )
            }
    }

    @Test
    fun uiState_whenUnfollowingAuthors_thenShowUpdatedAuthors() = runTest {
        viewModel.uiState
            .test {
                awaitItem()
                authorsRepository.sendAuthors(testOutputAuthors.map { it.author })
                userDataRepository.setFollowedAuthorIds(
                    setOf(testOutputAuthors[0].author.id, testOutputAuthors[1].author.id)
                )
                topicsRepository.sendTopics(listOf())
                userDataRepository.setFollowedTopicIds(setOf())

                awaitItem()
                viewModel.followAuthor(
                    followedAuthorId = testOutputAuthors[1].author.id,
                    followed = false
                )

                assertEquals(
                    InterestsUiState.Interests(topics = emptyList(), authors = testInputAuthors),
                    awaitItem()
                )
            }
    }
}

private const val TOPIC_1_NAME = "Android Studio"
private const val TOPIC_2_NAME = "Build"
private const val TOPIC_3_NAME = "Compose"
private const val TOPIC_SHORT_DESC = "At vero eos et accusamus."
private const val TOPIC_LONG_DESC = "At vero eos et accusamus et iusto odio dignissimos ducimus."
private const val TOPIC_URL = "URL"
private const val TOPIC_IMAGE_URL = "Image URL"

private val testInputAuthors = listOf(
    FollowableAuthor(
        Author(
            id = "0",
            name = "Android Dev",
            imageUrl = "",
            twitter = "",
            mediumPage = "",
            bio = "",
        ),
        isFollowed = true
    ),
    FollowableAuthor(
        Author(
            id = "1",
            name = "Android Dev 2",
            imageUrl = "",
            twitter = "",
            mediumPage = "",
            bio = "",
        ),
        isFollowed = false
    ),
    FollowableAuthor(
        Author(
            id = "2",
            name = "Android Dev 3",
            imageUrl = "",
            twitter = "",
            mediumPage = "",
            bio = "",
        ),
        isFollowed = false
    )
)

private val testOutputAuthors = listOf(
    FollowableAuthor(
        Author(
            id = "0",
            name = "Android Dev",
            imageUrl = "",
            twitter = "",
            mediumPage = "",
            bio = "",
        ),
        isFollowed = true
    ),
    FollowableAuthor(
        Author(
            id = "1",
            name = "Android Dev 2",
            imageUrl = "",
            twitter = "",
            mediumPage = "",
            bio = "",
        ),
        isFollowed = true
    ),
    FollowableAuthor(
        Author(
            id = "2",
            name = "Android Dev 3",
            imageUrl = "",
            twitter = "",
            mediumPage = "",
            bio = "",
        ),
        isFollowed = false
    )
)

private val testInputTopics = listOf(
    FollowableTopic(
        Topic(
            id = "0",
            name = TOPIC_1_NAME,
            shortDescription = TOPIC_SHORT_DESC,
            longDescription = TOPIC_LONG_DESC,
            url = TOPIC_URL,
            imageUrl = TOPIC_IMAGE_URL,
        ),
        isFollowed = true
    ),
    FollowableTopic(
        Topic(
            id = "1",
            name = TOPIC_2_NAME,
            shortDescription = TOPIC_SHORT_DESC,
            longDescription = TOPIC_LONG_DESC,
            url = TOPIC_URL,
            imageUrl = TOPIC_IMAGE_URL,
        ),
        isFollowed = false
    ),
    FollowableTopic(
        Topic(
            id = "2",
            name = TOPIC_3_NAME,
            shortDescription = TOPIC_SHORT_DESC,
            longDescription = TOPIC_LONG_DESC,
            url = TOPIC_URL,
            imageUrl = TOPIC_IMAGE_URL,
        ),
        isFollowed = false
    )
)

private val testOutputTopics = listOf(
    FollowableTopic(
        Topic(
            id = "0",
            name = TOPIC_1_NAME,
            shortDescription = TOPIC_SHORT_DESC,
            longDescription = TOPIC_LONG_DESC,
            url = TOPIC_URL,
            imageUrl = TOPIC_IMAGE_URL,
        ),
        isFollowed = true
    ),
    FollowableTopic(
        Topic(
            id = "1",
            name = TOPIC_2_NAME,
            shortDescription = TOPIC_SHORT_DESC,
            longDescription = TOPIC_LONG_DESC,
            url = TOPIC_URL,
            imageUrl = TOPIC_IMAGE_URL,
        ),
        isFollowed = true
    ),
    FollowableTopic(
        Topic(
            id = "2",
            name = TOPIC_3_NAME,
            shortDescription = TOPIC_SHORT_DESC,
            longDescription = TOPIC_LONG_DESC,
            url = TOPIC_URL,
            imageUrl = TOPIC_IMAGE_URL,
        ),
        isFollowed = false
    )
)
