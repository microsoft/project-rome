//-------------------------------------------------------------------------
//  <copyright file="UnsIdController.cs" company="Microsoft">
//      Copyright (c) Microsoft Corporation.  All rights reserved.
//  </copyright>
//  <project>GraphNotificationsUsingUnsId</project>
//  <summary>
//      Controller that gets the App Service Auth token and posts a notification
//      to the user via Microsoft Graph
//  </summary>
//-------------------------------------------------------------------------

using System;
using System.Collections.Generic;
using System.Configuration;
using System.Net;
using System.Net.Http;
using System.Net.Http.Formatting;
using System.Net.Http.Headers;
using System.Text;
using System.Threading.Tasks;
using System.Web.Http;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace Microsoft.Graph.Notifications.Sample.GraphNotificationsUsingUnsId.Controllers
{
    /// <summary>
    /// Controller that gets the App Service Auth token and posts a notification
    /// to the user via Microsoft Graph
    /// </summary>
    public class UnsIdController : ApiController
    {

        #region Private members

        private Uri graphBaseAddress = new Uri("https://graph.microsoft.com/");
        private Uri authBaseAddress = new Uri("https://login.microsoftonline.com/common/");

        #endregion

        #region Public methods

        /// <summary>
        /// Obtains the app service auth token and posts a notification as defined in the content
        /// </summary>
        /// <param name="content">The notification payload</param>
        /// <returns>The appropriate response of the POST/PUT operation</returns>
        [HttpPut, HttpPost]
        public async Task<HttpResponseMessage> PostAsync(JObject content)
        {
            string unsId = ConfigurationManager.AppSettings["UnsId"] ?? string.Empty;

            //  Get the App Service Auth Token
            HttpResponseMessage tokenRequest = await this.GetAppServiceAuthTokenAsync();
            dynamic appServiceAuthResponse = tokenRequest.Content.ReadAsAsync<object>(
                        new List<MediaTypeFormatter>{
                            new XmlMediaTypeFormatter(),
                            new JsonMediaTypeFormatter()}).Result;

            string appServiceAuthToken = string.Empty;
            if (appServiceAuthResponse != null)
            {
                appServiceAuthToken = appServiceAuthResponse["access_token"];
            }

            if (!string.IsNullOrWhiteSpace(appServiceAuthToken))
            {
                // Hit the Graph endpoint to send notification payload with App Service Auth token in the header
                using (HttpClient httpClient = new HttpClient { BaseAddress = graphBaseAddress })
                {
                    httpClient.DefaultRequestHeaders
                        .Accept
                        .Add(new MediaTypeWithQualityHeaderValue("application/json"));

                    httpClient.DefaultRequestHeaders.Authorization
                         = new AuthenticationHeaderValue("Bearer", appServiceAuthToken);

                    // Add the X-UNS-ID request header
                    httpClient.DefaultRequestHeaders.Add("X-UNS-ID", unsId);

                    StringContent httpContent = new StringContent(JsonConvert.SerializeObject(content), Encoding.UTF8, "application/json");

                    return await httpClient.PostAsync("beta/me/notifications", httpContent);
                }
            }

            return new HttpResponseMessage
            {
                Content = new StringContent(JsonConvert.SerializeObject(appServiceAuthResponse), Encoding.UTF8, "application/json"),
                StatusCode = HttpStatusCode.Unauthorized,
                ReasonPhrase = "Unauthorized to get App Service Auth token"
            };
        }

        #endregion

        #region Private methods

        /// <summary>
        /// Method to get App Service Auth token
        /// </summary>
        /// <returns>The App Service Auth Token or an HTTP response message indicating why the token could not be acquired</returns>
        private async Task<HttpResponseMessage> GetAppServiceAuthTokenAsync()
        {
            string clientId = ConfigurationManager.AppSettings["ida:AppServiceClientId"] ?? string.Empty;
            string clientKey = ConfigurationManager.AppSettings["ida:AppServiceSecret"] ?? string.Empty;


            if (string.IsNullOrWhiteSpace(clientId) || string.IsNullOrWhiteSpace(clientKey))
            {
                return new HttpResponseMessage(HttpStatusCode.Unauthorized);
            }

            Dictionary<string, string> nameValues = new Dictionary<string, string>();

            nameValues.Add("grant_type", "client_credentials");
            nameValues.Add("client_id", clientId);
            nameValues.Add("client_secret", clientKey);
            nameValues.Add("scope", graphBaseAddress + ".default");

            FormUrlEncodedContent content = new FormUrlEncodedContent(nameValues);

            using (HttpClient httpClient = new HttpClient { BaseAddress = authBaseAddress })
            {
                return await httpClient.PostAsync("oauth2/v2.0/token", content);
            }
        }

        #endregion
    }
}
