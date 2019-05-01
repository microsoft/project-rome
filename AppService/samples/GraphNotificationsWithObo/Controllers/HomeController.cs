//-----------------------------------------------------------------------
// <copyright file="HomeController.cs" company="Microsoft">
//     Copyright (c) Microsoft Corporation.  All rights reserved.
// </copyright>
//-----------------------------------------------------------------------

using System.Web.Mvc;

namespace Microsoft.Graph.Notifications.Sample.Controllers
{

    public class HomeController : Controller
    {
        public ActionResult Index()
        {
            return View("Graph");
        }
    }
}