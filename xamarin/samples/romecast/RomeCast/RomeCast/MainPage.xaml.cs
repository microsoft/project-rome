//*********************************************************
//
// Copyright (c) Microsoft. All rights reserved.
// This code is licensed under the MIT License (MIT).
// THIS CODE IS PROVIDED *AS IS* WITHOUT WARRANTY OF
// ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING ANY
// IMPLIED WARRANTIES OF FITNESS FOR A PARTICULAR
// PURPOSE, MERCHANTABILITY, OR NON-INFRINGEMENT.
//
//*********************************************************

using System;
using System.Diagnostics;
using System.Linq;
using System.Threading.Tasks;
using Windows.Foundation;
using Windows.Media.Core;
using Windows.Media.Playback;
using Windows.UI.Core;
using Windows.UI.Popups;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Media.Imaging;
using Windows.UI.Xaml.Navigation;
using Windows.Web;
using Windows.Web.Syndication;

// The Blank Page item template is documented at https://go.microsoft.com/fwlink/?LinkId=402352&clcid=0x409

namespace RomeCast
{
    /// <summary>
    /// An empty page that can be used on its own or navigated to within a Frame.
    /// </summary>
    public sealed partial class MainPage : Page
    {
        private MediaPlaybackList playbackList = new MediaPlaybackList();
        private MediaPlayer mediaPlayer;

        public MainPage()
        {
            this.InitializeComponent();
        }

        protected override async void OnNavigatedTo(NavigationEventArgs e)
        {
            if (mediaPlayer == null)
            {
                // Create a static playback list
                await InitializePlaybackList();

                //Create the MediaPlayer:
                mediaPlayer = new MediaPlayer();

                // Subscribe to MediaPlayer PlaybackState changed events
                mediaPlayer.PlaybackSession.PlaybackStateChanged += PlaybackSession_PlaybackStateChanged;
                mediaPlayer.PlaybackSession.BufferingEnded += PlaybackSession_BufferingEnded;
                mediaPlayer.PlaybackSession.BufferingStarted += PlaybackSession_BufferingStarted;


                // Subscribe to list UI changes
                playlistView.ItemClick += PlaylistView_ItemClick;

                //Attach the player to the MediaPlayerElement:
                mediaPlayerElement.SetMediaPlayer(mediaPlayer);

                // Set list for playback
                mediaPlayer.Source = playbackList;
            }
        }


        internal void HandleCommand(string commandString)
        {
            if (commandString != null)
            {
                commandString = commandString.TrimStart('?').ToLower();

                switch (commandString)
                {
                    //Commands
                    case "play": mediaPlayer.Play(); break;
                    case "pause": mediaPlayer.Pause(); break;
                    case "prev": playbackList.MovePrevious(); break;
                    case "next": playbackList.MoveNext(); break;
                }
            }
        }

        private async void PlaybackSession_BufferingStarted(MediaPlaybackSession sender, object args)
        {
            Debug.WriteLine("PlaybackSession_BufferingStarted");

            await Dispatcher.RunAsync(CoreDispatcherPriority.Normal, () => LoadingTextBlock.Visibility = Visibility.Visible);
        }

        private async void PlaybackSession_BufferingEnded(MediaPlaybackSession sender, object args)
        {
            Debug.WriteLine("PlaybackSession_BufferingEnded");
            await Dispatcher.RunAsync(CoreDispatcherPriority.Normal, () => LoadingTextBlock.Visibility = Visibility.Collapsed);

        }

        public async Task InitializePlaybackList()
        {
            // string uri = "https://channel9.msdn.com/Feeds/RSS"; //"http://www.xamarinpodcast.com/rss";
            string uri = "http://www.xamarinpodcast.com/rss";

            Uri _feedUri;

            if (!Uri.TryCreate(uri, UriKind.Absolute, out _feedUri))
            {
                return;
            }

            var client = new SyndicationClient { BypassCacheOnRetrieve = true };
            try
            {
                var _currentFeed = await client.RetrieveFeedAsync(_feedUri);

                var title = _currentFeed.Title;

                foreach (var item in _currentFeed.Items)
                {
                    // Display title.
                    var displayTitle = item.Title?.Text ?? "(no title)";
                    if (item.Links.Count > 1)
                    {
                        var linkUri = item.Links[1].Uri;
                        var artUriString = item.ElementExtensions.FirstOrDefault(e => e.NodeName == "image")?.AttributeExtensions.FirstOrDefault(a => a.Name == "href")?.Value;
                        if (string.IsNullOrEmpty(artUriString))
                        {
                            artUriString = item.ElementExtensions.LastOrDefault(e => e.NodeName == "thumbnail")?.AttributeExtensions.FirstOrDefault(a => a.Name == "url")?.Value;
                        }

                        Uri artUri = null;
                        if (artUriString != null)
                            artUri = new Uri(artUriString);

                        var media = new MediaModel(linkUri) { Title = displayTitle, ArtUri = artUri };

                        playlistView.Items.Add(media);
                        playbackList.Items.Add(media.MediaPlaybackItem);
                    }
                }
            }
            catch (Exception ex)
            {
                SyndicationErrorStatus status = SyndicationError.GetStatus(ex.HResult);
                if (status == SyndicationErrorStatus.InvalidXml)
                {
                    Debug.WriteLine("An invalid XML exception was thrown. " +
                                    "Please make sure to use a URI that points to a RSS or Atom feed.");
                }

                if (status == SyndicationErrorStatus.Unknown)
                {
                    WebErrorStatus webError = WebError.GetStatus(ex.HResult);

                    if (webError == WebErrorStatus.Unknown)
                    {
                        // Neither a syndication nor a web error. Rethrow.
                        throw;
                    }
                }
                Debug.WriteLine(ex.Message);
            }

            // Subscribe for changes
            playbackList.CurrentItemChanged += PlaybackList_CurrentItemChanged;

            // Loop
            playbackList.AutoRepeatEnabled = true;

            playlistView.SelectedIndex = -1;
        }


        protected override void OnNavigatedFrom(NavigationEventArgs e)
        {
            MediaPlayerHelper.CleanUpMediaPlayerSource(mediaPlayer);
            playbackList.Items.Clear();
            playlistView.Items.Clear();
        }

        private void PlaybackList_CurrentItemChanged(MediaPlaybackList sender, CurrentMediaPlaybackItemChangedEventArgs args)
        {
            var task = Dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
            {
                // Synchronize our UI with the currently-playing item.
                playlistView.SelectedIndex = (int)sender.CurrentItemIndex;
            });
        }

        /// <summary>
        /// MediaPlayer state changed event handlers.
        /// Note that we can subscribe to events even if Media Player is playing media in background
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="args"></param>
        ///
        private async void PlaybackSession_PlaybackStateChanged(MediaPlaybackSession sender, object args)
        {
            await this.Dispatcher.RunAsync(CoreDispatcherPriority.Normal, () =>
            {
                var currentState = sender.PlaybackState;

                // Update controls
                UpdateTransportControls(currentState);
            });
        }

        private void PlaylistView_ItemClick(object sender, ItemClickEventArgs e)
        {
            var item = e.ClickedItem as MediaModel;
            Debug.WriteLine("Clicked item: " + item.Title);

            // Start the background task if it wasn't running
            playbackList.MoveTo((uint)playbackList.Items.IndexOf(item.MediaPlaybackItem));

            //set poster frame
            BitmapImage image = new BitmapImage();
            image.UriSource = item.ArtUri;
            mediaPlayerElement.PosterSource = image;

            if (MediaPlaybackState.Paused == mediaPlayer.PlaybackSession.PlaybackState)
            {
                mediaPlayer.Play();
            }
        }

        /// <summary>
        /// Sends message to the background task to skip to the previous track.
        /// </summary>
        private void prevButton_Click(object sender, RoutedEventArgs e)
        {
            playbackList.MovePrevious();
        }

        /// <summary>
        /// If the task is already running, it will just play/pause MediaPlayer Instance
        /// Otherwise, initializes MediaPlayer Handlers and starts playback
        /// track or to pause if we're already playing.
        /// </summary>
        private void playButton_Click(object sender, RoutedEventArgs e)
        {
            if (MediaPlaybackState.Playing == mediaPlayer.PlaybackSession.PlaybackState)
            {
                mediaPlayer.Pause();
            }
            else if (MediaPlaybackState.Paused == mediaPlayer.PlaybackSession.PlaybackState)
            {
                mediaPlayer.Play();
            }
        }

        /// <summary>
        /// Tells the background audio agent to skip to the next track.
        /// </summary>
        /// <param name="sender">The button</param>
        /// <param name="e">Click event args</param>
        private void nextButton_Click(object sender, RoutedEventArgs e)
        {
            playbackList.MoveNext();
        }

        private async void speedButton_Click(object sender, RoutedEventArgs e)
        {
            // Create menu and add commands
            var popupMenu = new PopupMenu();

            popupMenu.Commands.Add(new UICommand("4.0x", command => mediaPlayer.PlaybackSession.PlaybackRate = 4.0));
            popupMenu.Commands.Add(new UICommand("2.0x", command => mediaPlayer.PlaybackSession.PlaybackRate = 2.0));
            popupMenu.Commands.Add(new UICommand("1.5x", command => mediaPlayer.PlaybackSession.PlaybackRate = 1.5));
            popupMenu.Commands.Add(new UICommand("1.0x", command => mediaPlayer.PlaybackSession.PlaybackRate = 1.0));
            popupMenu.Commands.Add(new UICommand("0.5x", command => mediaPlayer.PlaybackSession.PlaybackRate = 0.5));

            // Get button transform and then offset it by half the button
            // width to center. This will show the popup just above the button.
            var button = (Button)sender;
            var transform = button.TransformToVisual(null);
            var point = transform.TransformPoint(new Point(button.ActualWidth / 2, 0));

            // Show popup
            IUICommand result = await popupMenu.ShowAsync(point);
            if (result != null)
            {
                button.Content = result.Label;
            }
        }

        private void UpdateTransportControls(MediaPlaybackState state)
        {
            nextButton.IsEnabled = true;
            prevButton.IsEnabled = true;
            if (state == MediaPlaybackState.Playing)
            {
                playButtonSymbol.Symbol = Symbol.Pause;
            }
            else
            {
                playButtonSymbol.Symbol = Symbol.Play;
            }
        }
    }

    public class MediaModel
    {
        public MediaModel(Uri mediaUri)
        {
            MediaPlaybackItem = new MediaPlaybackItem(MediaSource.CreateFromUri(mediaUri));
        }

        public string Title { get; set; }
        public Uri ArtUri { get; set; }
        public MediaPlaybackItem MediaPlaybackItem { get; private set; }
    }

    public static class MediaPlayerHelper
    {
        public static void CleanUpMediaPlayerSource(Windows.Media.Playback.MediaPlayer mp)
        {
            if (mp?.Source != null)
            {
                var source = mp.Source as Windows.Media.Core.MediaSource;
                source?.Dispose();

                var item = mp.Source as Windows.Media.Playback.MediaPlaybackItem;
                item?.Source?.Dispose();

                mp.Source = null;
            }
        }
    }
}
