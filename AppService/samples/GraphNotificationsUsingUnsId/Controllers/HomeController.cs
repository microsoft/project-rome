//-------------------------------------------------------------------------
//  <copyright file="HomeController.cs" company="Microsoft">
//      Copyright (c) Microsoft Corporation.  All rights reserved.
//  </copyright>
//  <project>GraphNotificationsUsingUnsId</project>
//  <summary>
//      Main controller that gets called when the project runs after initialization
//  </summary>
//-------------------------------------------------------------------------

using System.IO;
using System.Net.Http;
using System.Threading.Tasks;
using System.Web.Http;
using Newtonsoft.Json.Linq;

namespace Microsoft.Graph.Notifications.Sample.GraphNotificationsUsingUnsId.Controllers
{
    /// <summary>
    /// Main controller that gets called when the project runs after initialization
    /// Reads the JSON payload from App_Data\Payload.json and calls the UnsIdController's 
    /// PostAsync() method to post the notification payload
    /// </summary>
    public class HomeController : ApiController
    {
        [HttpGet]
        public async Task<HttpResponseMessage> IndexAsync()
        {
            // Read the JSON payload
            string payload = File.ReadAllText(@"App_Data\Payload.json");
            JObject json = JObject.Parse(payload);

            // Call the UnsIdController's PostAsync() method to post the notification payload
            return await new UnsIdController().PostAsync(json);
        }
    }
}
