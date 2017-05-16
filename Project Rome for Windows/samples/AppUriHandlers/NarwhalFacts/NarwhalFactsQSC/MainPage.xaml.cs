using Windows.ApplicationModel.Activation;
using Windows.UI;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Media;
using Windows.UI.Xaml.Navigation;

namespace NarwhalFactsSelfhost
{
    /// <summary>
    /// An empty page that can be used on its own or navigated to within a Frame.
    /// </summary>
    public sealed partial class MainPage : Page
    {

        public MainPage()
        {
            this.InitializeComponent();
        }


        private void ShowSplitView(object sender, RoutedEventArgs e)
        {
            MySidePanel.SideSplitView.IsPaneOpen = !MySidePanel.SideSplitView.IsPaneOpen;
        }

        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
            var args = e.Parameter as ProtocolActivatedEventArgs;

            if (args != null)
            {
                LaunchStatusTextBlock.Text = "Success! AppUriHandler Launch";
                LaunchStatusTextBlock.Foreground = new SolidColorBrush(Colors.Green);
                LaunchUriDisplayTextBlock.Text = args.Uri.AbsoluteUri;
            }
        }
    }
}
