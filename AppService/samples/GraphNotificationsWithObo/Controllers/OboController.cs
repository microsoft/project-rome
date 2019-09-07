//-----------------------------------------------------------------------
// <copyright file="OboController.cs" company="Microsoft">
//     Copyright (c) Microsoft Corporation.  All rights reserved.
// </copyright>
//-----------------------------------------------------------------------

using System;
using System.Collections.Generic;
using System.Configuration;
using System.Net;
using System.Net.Http;
using System.Net.Http.Formatting;
using System.Net.Http.Headers;
using System.Text;
using System.Threading.Tasks;
using System.Web;
using System.Web.Http;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace Microsoft.Graph.Notifications.Sample.Controllers
{
    public class OboController : ApiController
    {
        private Uri graphBaseAddress = new Uri("https://graph.microsoft.com/");
        private Uri baseAddress = new Uri("https://login.microsoftonline.com/common/");
        HttpContext httpContext = HttpContext.Current;

        /// <summary>
        /// Step6: App Service Logic
        /// This is the main logic to create notification through graph notification endpoint.
        /// First: Exchange the access token with the right scopes for getting the On-Behalf-Of (OBO) access token.
        /// Seond: Send notification payload through Graph endpoint with OBO token in header.
        /// Please Note: The App Service needs to maintain the OBO AT/RT for each client. When AT expires (every one hour), 
        /// the RT can be used to get a new AT.
        /// </summary>
        [HttpPut, HttpPost]
        public async Task<HttpResponseMessage> PostAsync(JObject content)
        {
            // Extract sample App authorization access token from header 
            // which exchange for accessing on-behalf-of token.
            var token = this.httpContext.Request?.Headers["Authorization"];
            if (string.IsNullOrWhiteSpace(token) ||
                token.IndexOf("Bearer ", StringComparison.OrdinalIgnoreCase) < 0)
            {
                return new HttpResponseMessage(HttpStatusCode.Unauthorized);
            }

            var tokenValue = token.Substring("Bearer ".Length);
            var clientId = this.httpContext.Request?.Headers["ClientId"];

            //Get On-Behalf-of token for App Service using user's access token 
            var tokenRequest = await this.GetOboTokenAsync(tokenValue, clientId);
            dynamic oboResponse = tokenRequest.Content.ReadAsAsync<object>(
                        new List<MediaTypeFormatter>{
                            new XmlMediaTypeFormatter(),
                            new JsonMediaTypeFormatter()}).Result;

            string oboToken = string.Empty;
            if (oboResponse != null)
            {
                oboToken = oboResponse["access_token"];
            }

            if (!string.IsNullOrWhiteSpace(oboToken))
            {
                // Hit the Graph endpoint to send notification payload with OBO token in header.
                using (var httpClient = new HttpClient { BaseAddress = graphBaseAddress })
                {
                    httpClient.DefaultRequestHeaders
                        .Accept
                        .Add(new MediaTypeWithQualityHeaderValue("application/json"));

                    httpClient.DefaultRequestHeaders.Authorization
                         = new AuthenticationHeaderValue("Bearer", oboToken);

                    var httpContent = new StringContent(JsonConvert.SerializeObject(content), Encoding.UTF8, "application/json");

                    return await httpClient.PostAsync("beta/me/notifications", httpContent);
                }
            }
            return new HttpResponseMessage
            {
                Content = new StringContent(JsonConvert.SerializeObject(oboResponse), Encoding.UTF8, "application/json"),
                StatusCode = HttpStatusCode.Unauthorized,
                ReasonPhrase = "Unauthorized to get On-Behalf-Of token"
            };
        }

        /// <summary>
        /// Method to get OBO token for user's AT 
        /// </summary>
        /// <param name="token">User's Access Token</param>
        /// <returns>OBO Token</returns>
        private async Task<HttpResponseMessage> GetOboTokenAsync(string token, string clientId)
        {
            string clientKey = ConfigurationManager.AppSettings["ida:AppServiceSecret"] ?? string.Empty;

            if (string.IsNullOrWhiteSpace(clientId) || string.IsNullOrWhiteSpace(clientKey))
            {
                return new HttpResponseMessage(HttpStatusCode.Unauthorized);
            }

            var nameValues = new Dictionary<string, string>();

            nameValues.Add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
            nameValues.Add("client_id", clientId);
            nameValues.Add("client_secret", clientKey);
            nameValues.Add("assertion", token);
            nameValues.Add("scope", graphBaseAddress + "User.Read " + graphBaseAddress + "Notifications.ReadWrite.CreatedByApp");
            nameValues.Add("requested_token_use", "on_behalf_of");

            var content = new FormUrlEncodedContent(nameValues);

            using (var httpClient = new HttpClient { BaseAddress = baseAddress })
            {
                return await httpClient.PostAsync("oauth2/v2.0/token", content);
            }
        }
    }
}
